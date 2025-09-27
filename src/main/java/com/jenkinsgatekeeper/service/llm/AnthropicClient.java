package com.jenkinsgatekeeper.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenkinsgatekeeper.config.GatekeeperConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AnthropicClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AnthropicClient.class);
    
    private final GatekeeperConfig config;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    
    public AnthropicClient(GatekeeperConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is required");
        }
        logger.debug("AnthropicClient initialized with API key: {}...", 
            apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : "***");
    }
    
    public String reviewCode(String prompt) throws Exception {
        logger.info("Sending code review request to Anthropic ({} chars)", prompt.length());
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://api.anthropic.com/v1/messages");
            request.setHeader("x-api-key", apiKey);
            request.setHeader("anthropic-version", "2023-06-01");
            request.setHeader("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("messages", List.of(Map.of(
                "role", "user",
                "content", prompt
            )));
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                
                if (statusCode >= 200 && statusCode < 300) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
                    if (content != null && !content.isEmpty()) {
                        String text = (String) content.get(0).get("text");
                        logger.info("Received response from Anthropic ({} chars)", text.length());
                        return text;
                    }
                }
                
                throw new RuntimeException("API request failed with status " + statusCode + ": " + responseBody);
            });
            
        } catch (Exception e) {
            logger.error("Failed to get review from Anthropic: {}", e.getMessage());
            throw e;
        }
    }
    
    public String reviewCodeStreaming(String prompt) throws Exception {
        // For now, use non-streaming implementation
        // Streaming can be implemented later with Server-Sent Events
        logger.info("Streaming not implemented, falling back to regular request");
        return reviewCode(prompt);
    }
    
    public String reviewCodeWithRetry(String prompt) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= config.getMaxRetries(); attempt++) {
            try {
                logger.info("Attempt {} of {} to get review from Anthropic", attempt, config.getMaxRetries());
                return reviewCode(prompt);
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < config.getMaxRetries()) {
                    // Exponential backoff
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    logger.info("Retrying in {} ms", delay);
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new Exception("All retry attempts failed", lastException);
    }
}
