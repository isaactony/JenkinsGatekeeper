package com.jenkinsgatekeeper.service;

import com.jenkinsgatekeeper.config.GatekeeperConfig;
import com.jenkinsgatekeeper.model.ReviewResult;
import com.jenkinsgatekeeper.service.diff.DiffCollector;
import com.jenkinsgatekeeper.service.filter.PreFilterEngine;
import com.jenkinsgatekeeper.service.llm.AnthropicClient;
import com.jenkinsgatekeeper.service.llm.LlmResponseParser;
import com.jenkinsgatekeeper.service.policy.PolicyEngine;
import com.jenkinsgatekeeper.service.report.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class GatekeeperService {
    
    private static final Logger logger = LoggerFactory.getLogger(GatekeeperService.class);
    
    private final GatekeeperConfig config;
    private final DiffCollector diffCollector;
    private final PreFilterEngine preFilterEngine;
    private final AnthropicClient anthropicClient;
    private final LlmResponseParser responseParser;
    private final PolicyEngine policyEngine;
    private final ReportGenerator reportGenerator;
    
    public GatekeeperService(GatekeeperConfig config) {
        this.config = config;
        this.diffCollector = new DiffCollector();
        this.preFilterEngine = new PreFilterEngine(config);
        this.anthropicClient = new AnthropicClient(config);
        this.responseParser = new LlmResponseParser();
        this.policyEngine = new PolicyEngine(config);
        this.reportGenerator = new ReportGenerator();
    }
    
    public ReviewResult reviewDiff(String diffContent, Path workspace) throws Exception {
        logger.info("Starting AI review for diff ({} bytes)", diffContent.length());
        
        // Step 1: Parse and analyze the diff
        Map<String, Object> diffAnalysis = diffCollector.analyzeDiff(diffContent);
        logger.info("Diff analysis complete: {} files changed", 
            ((List<?>) diffAnalysis.get("changedFiles")).size());
        
        // Step 2: Run pre-filters
        Map<String, Object> preFilterResults = preFilterEngine.runFilters(diffContent, diffAnalysis);
        logger.info("Pre-filters complete: {} secret hits, {} banlist hits", 
            ((List<?>) preFilterResults.get("secretHits")).size(),
            ((List<?>) preFilterResults.get("banlistHits")).size());
        
        // Step 3: Generate LLM prompt and get review
        String prompt = buildPrompt(diffAnalysis, preFilterResults, workspace);
        String llmResponse = anthropicClient.reviewCodeWithRetry(prompt);
        
        // Step 4: Parse LLM response
        ReviewResult result = responseParser.parseResponse(llmResponse);
        
        // Step 5: Apply policy engine for final verdict
        result = policyEngine.evaluateResult(result, preFilterResults);
        
        logger.info("Review complete: verdict={}, risk_score={}, findings={}", 
            result.getVerdict(), result.getRiskScore(), result.getFindings().size());
        
        return result;
    }
    
    private String buildPrompt(Map<String, Object> diffAnalysis, Map<String, Object> preFilterResults, Path workspace) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a CI gatekeeper for secure engineering. Analyze the following code changes and return ONLY valid JSON matching the schema.\n\n");
        
        // Repository context
        prompt.append("Repository: ").append(workspace.getFileName()).append("\n");
        prompt.append("Languages: ").append(String.join(", ", config.getLanguages())).append("\n");
        prompt.append("Coverage minimum: ").append(config.getCoverageMin()).append("\n\n");
        
        // Pre-filter findings
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secretHits = (List<Map<String, Object>>) preFilterResults.get("secretHits");
        if (!secretHits.isEmpty()) {
            prompt.append("SECRET DETECTION HITS:\n");
            secretHits.forEach(hit -> {
                prompt.append("- ").append(hit.get("rule")).append(" in ").append(hit.get("file"))
                      .append(":").append(hit.get("line")).append("\n");
            });
            prompt.append("\n");
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> banlistHits = (List<Map<String, Object>>) preFilterResults.get("banlistHits");
        if (!banlistHits.isEmpty()) {
            prompt.append("INSECURE PATTERN HITS:\n");
            banlistHits.forEach(hit -> {
                prompt.append("- ").append(hit.get("pattern")).append(" in ").append(hit.get("file"))
                      .append(":").append(hit.get("line")).append("\n");
            });
            prompt.append("\n");
        }
        
        // Changed files
        @SuppressWarnings("unchecked")
        List<String> changedFiles = (List<String>) diffAnalysis.get("changedFiles");
        prompt.append("CHANGED FILES:\n");
        changedFiles.forEach(file -> prompt.append("- ").append(file).append("\n"));
        prompt.append("\n");
        
        // Diff content (truncated if too large)
        String diffContent = (String) diffAnalysis.get("content");
        if (diffContent.length() > 10000) {
            prompt.append("DIFF (truncated):\n").append(diffContent.substring(0, 10000)).append("\n...\n");
        } else {
            prompt.append("DIFF:\n").append(diffContent).append("\n");
        }
        
        prompt.append("\nAnalyze for:\n");
        prompt.append("1. Security vulnerabilities (secrets, insecure APIs, injection risks)\n");
        prompt.append("2. Code quality issues (complexity, maintainability)\n");
        prompt.append("3. Missing tests for changed code\n");
        prompt.append("4. Performance concerns\n\n");
        
        prompt.append("Return JSON with this exact schema:\n");
        prompt.append("{\n");
        prompt.append("  \"risk_score\": 0-100,\n");
        prompt.append("  \"verdict\": \"pass|warn|fail\",\n");
        prompt.append("  \"findings\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": \"SEC-001\",\n");
        prompt.append("      \"title\": \"Issue title\",\n");
        prompt.append("      \"severity\": \"INFO|LOW|MEDIUM|HIGH|CRITICAL\",\n");
        prompt.append("      \"file\": \"path/to/file\",\n");
        prompt.append("      \"start_line\": 1,\n");
        prompt.append("      \"end_line\": 1,\n");
        prompt.append("      \"rationale\": \"Why this is an issue\",\n");
        prompt.append("      \"fix_suggestion\": \"How to fix it\",\n");
        prompt.append("      \"patch\": \"diff snippet or code\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"missing_tests\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"file_under_test\": \"path/to/file\",\n");
        prompt.append("      \"suggested_test_file\": \"path/to/test\",\n");
        prompt.append("      \"test_stub\": \"test code\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    
    public void generateSarifReport(ReviewResult result, Path outputPath) throws Exception {
        reportGenerator.generateSarifReport(result, outputPath);
    }
}
