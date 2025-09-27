package com.jenkinsgatekeeper.service.policy;

import com.jenkinsgatekeeper.config.GatekeeperConfig;
import com.jenkinsgatekeeper.model.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class PolicyEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyEngine.class);
    
    private final GatekeeperConfig config;
    
    public PolicyEngine(GatekeeperConfig config) {
        this.config = config;
    }
    
    public ReviewResult evaluateResult(ReviewResult result, Map<String, Object> preFilterResults) {
        logger.info("Evaluating result with policy engine");
        
        // Calculate risk score based on findings
        int riskScore = calculateRiskScore(result, preFilterResults);
        result.setRiskScore(riskScore);
        
        // Determine verdict based on thresholds
        ReviewResult.Verdict verdict = determineVerdict(riskScore, result, preFilterResults);
        result.setVerdict(verdict);
        
        logger.info("Policy evaluation complete: risk_score={}, verdict={}", riskScore, verdict);
        
        return result;
    }
    
    private int calculateRiskScore(ReviewResult result, Map<String, Object> preFilterResults) {
        int maxSeverityScore = 0;
        
        // Calculate based on findings
        for (ReviewResult.Finding finding : result.getFindings()) {
            int severityScore = finding.getSeverity().getScore();
            maxSeverityScore = Math.max(maxSeverityScore, severityScore);
        }
        
        // Check for secret hits (always high risk)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secretHits = (List<Map<String, Object>>) preFilterResults.get("secretHits");
        if (!secretHits.isEmpty()) {
            maxSeverityScore = Math.max(maxSeverityScore, 100); // CRITICAL
        }
        
        // Check for insecure pattern hits
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> banlistHits = (List<Map<String, Object>>) preFilterResults.get("banlistHits");
        if (!banlistHits.isEmpty()) {
            maxSeverityScore = Math.max(maxSeverityScore, 80); // HIGH
        }
        
        // Check coverage requirements
        @SuppressWarnings("unchecked")
        Map<String, Object> coverage = (Map<String, Object>) preFilterResults.get("coverage");
        if (coverage != null) {
            Double overallCoverage = (Double) coverage.get("overall");
            if (overallCoverage != null && overallCoverage < config.getCoverageMin()) {
                maxSeverityScore = Math.max(maxSeverityScore, 60); // MEDIUM
            }
        }
        
        // Use the higher of model risk score or max severity
        return Math.max(result.getRiskScore(), maxSeverityScore);
    }
    
    private ReviewResult.Verdict determineVerdict(int riskScore, ReviewResult result, Map<String, Object> preFilterResults) {
        // Check for critical issues that should always fail
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secretHits = (List<Map<String, Object>>) preFilterResults.get("secretHits");
        if (!secretHits.isEmpty()) {
            logger.warn("Secret detection hits found - verdict: FAIL");
            return ReviewResult.Verdict.FAIL;
        }
        
        // Check for high severity findings
        boolean hasHighSeverity = result.getFindings().stream()
            .anyMatch(finding -> finding.getSeverity() == ReviewResult.Finding.Severity.HIGH ||
                               finding.getSeverity() == ReviewResult.Finding.Severity.CRITICAL);
        
        if (hasHighSeverity) {
            logger.warn("High severity findings detected - verdict: FAIL");
            return ReviewResult.Verdict.FAIL;
        }
        
        // Apply thresholds
        if (riskScore >= config.getFailThreshold()) {
            logger.warn("Risk score {} exceeds fail threshold {} - verdict: FAIL", 
                riskScore, config.getFailThreshold());
            return ReviewResult.Verdict.FAIL;
        }
        
        if (riskScore >= config.getWarnThreshold()) {
            logger.info("Risk score {} exceeds warn threshold {} - verdict: WARN", 
                riskScore, config.getWarnThreshold());
            return ReviewResult.Verdict.WARN;
        }
        
        logger.info("Risk score {} within acceptable range - verdict: PASS", riskScore);
        return ReviewResult.Verdict.PASS;
    }
    
    public boolean shouldRequireTests(String filePath) {
        return config.getRequireTestsForPaths().stream()
            .anyMatch(pattern -> matchesPattern(filePath, pattern));
    }
    
    private boolean matchesPattern(String filePath, String pattern) {
        // Simple pattern matching - in production, use proper glob matching
        if (pattern.contains("**")) {
            String prefix = pattern.substring(0, pattern.indexOf("**"));
            return filePath.startsWith(prefix);
        } else if (pattern.contains("*")) {
            // Simple wildcard matching
            String[] parts = pattern.split("\\*");
            if (parts.length == 2) {
                return filePath.startsWith(parts[0]) && filePath.endsWith(parts[1]);
            }
        } else {
            return filePath.equals(pattern);
        }
        return false;
    }
}
