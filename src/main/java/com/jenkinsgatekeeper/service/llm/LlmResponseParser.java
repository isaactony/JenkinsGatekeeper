package com.jenkinsgatekeeper.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenkinsgatekeeper.model.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LlmResponseParser {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmResponseParser.class);
    
    private final ObjectMapper objectMapper;
    
    public LlmResponseParser() {
        this.objectMapper = new ObjectMapper();
    }
    
    public ReviewResult parseResponse(String response) throws Exception {
        logger.info("Parsing LLM response ({} chars)", response.length());
        
        try {
            // Extract JSON from response (in case there's extra text)
            String jsonContent = extractJsonFromResponse(response);
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            ReviewResult result = new ReviewResult();
            
            // Parse risk score
            if (rootNode.has("risk_score")) {
                result.setRiskScore(rootNode.get("risk_score").asInt());
            }
            
            // Parse verdict
            if (rootNode.has("verdict")) {
                String verdictStr = rootNode.get("verdict").asText().toUpperCase();
                try {
                    result.setVerdict(ReviewResult.Verdict.valueOf(verdictStr));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid verdict: {}, defaulting to PASS", verdictStr);
                    result.setVerdict(ReviewResult.Verdict.PASS);
                }
            }
            
            // Parse findings
            if (rootNode.has("findings")) {
                List<ReviewResult.Finding> findings = parseFindings(rootNode.get("findings"));
                result.setFindings(findings);
            }
            
            // Parse missing tests
            if (rootNode.has("missing_tests")) {
                List<ReviewResult.MissingTest> missingTests = parseMissingTests(rootNode.get("missing_tests"));
                result.setMissingTests(missingTests);
            }
            
            logger.info("Parsed response: risk_score={}, verdict={}, findings={}, missing_tests={}", 
                result.getRiskScore(), result.getVerdict(), 
                result.getFindings().size(), result.getMissingTests().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to parse LLM response", e);
            throw new Exception("Invalid LLM response format: " + e.getMessage(), e);
        }
    }
    
    private String extractJsonFromResponse(String response) {
        // Look for JSON content between curly braces
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        // If no JSON found, return the original response
        return response;
    }
    
    private List<ReviewResult.Finding> parseFindings(JsonNode findingsNode) {
        List<ReviewResult.Finding> findings = new ArrayList<>();
        
        if (findingsNode.isArray()) {
            for (JsonNode findingNode : findingsNode) {
                try {
                    ReviewResult.Finding finding = new ReviewResult.Finding();
                    
                    if (findingNode.has("id")) {
                        finding.setId(findingNode.get("id").asText());
                    }
                    
                    if (findingNode.has("title")) {
                        finding.setTitle(findingNode.get("title").asText());
                    }
                    
                    if (findingNode.has("severity")) {
                        String severityStr = findingNode.get("severity").asText().toUpperCase();
                        try {
                            finding.setSeverity(ReviewResult.Finding.Severity.valueOf(severityStr));
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid severity: {}, defaulting to INFO", severityStr);
                            finding.setSeverity(ReviewResult.Finding.Severity.INFO);
                        }
                    }
                    
                    if (findingNode.has("file")) {
                        finding.setFile(findingNode.get("file").asText());
                    }
                    
                    if (findingNode.has("start_line")) {
                        finding.setStartLine(findingNode.get("start_line").asInt());
                    }
                    
                    if (findingNode.has("end_line")) {
                        finding.setEndLine(findingNode.get("end_line").asInt());
                    } else if (findingNode.has("start_line")) {
                        finding.setEndLine(findingNode.get("start_line").asInt());
                    }
                    
                    if (findingNode.has("rationale")) {
                        finding.setRationale(findingNode.get("rationale").asText());
                    }
                    
                    if (findingNode.has("fix_suggestion")) {
                        finding.setFixSuggestion(findingNode.get("fix_suggestion").asText());
                    }
                    
                    if (findingNode.has("patch")) {
                        finding.setPatch(findingNode.get("patch").asText());
                    }
                    
                    findings.add(finding);
                    
                } catch (Exception e) {
                    logger.warn("Failed to parse finding: {}", e.getMessage());
                }
            }
        }
        
        return findings;
    }
    
    private List<ReviewResult.MissingTest> parseMissingTests(JsonNode missingTestsNode) {
        List<ReviewResult.MissingTest> missingTests = new ArrayList<>();
        
        if (missingTestsNode.isArray()) {
            for (JsonNode testNode : missingTestsNode) {
                try {
                    ReviewResult.MissingTest missingTest = new ReviewResult.MissingTest();
                    
                    if (testNode.has("file_under_test")) {
                        missingTest.setFileUnderTest(testNode.get("file_under_test").asText());
                    }
                    
                    if (testNode.has("suggested_test_file")) {
                        missingTest.setSuggestedTestFile(testNode.get("suggested_test_file").asText());
                    }
                    
                    if (testNode.has("test_stub")) {
                        missingTest.setTestStub(testNode.get("test_stub").asText());
                    }
                    
                    missingTests.add(missingTest);
                    
                } catch (Exception e) {
                    logger.warn("Failed to parse missing test: {}", e.getMessage());
                }
            }
        }
        
        return missingTests;
    }
}
