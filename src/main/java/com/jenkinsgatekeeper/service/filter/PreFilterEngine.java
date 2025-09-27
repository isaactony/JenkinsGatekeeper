package com.jenkinsgatekeeper.service.filter;

import com.jenkinsgatekeeper.config.GatekeeperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreFilterEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(PreFilterEngine.class);
    
    private final GatekeeperConfig config;
    
    public PreFilterEngine(GatekeeperConfig config) {
        this.config = config;
    }
    
    public Map<String, Object> runFilters(String diffContent, Map<String, Object> diffAnalysis) {
        logger.info("Running pre-filters on diff content");
        
        Map<String, Object> results = new HashMap<>();
        
        // Run secret detection
        List<Map<String, Object>> secretHits = detectSecrets(diffContent);
        results.put("secretHits", secretHits);
        
        // Run insecure pattern detection
        List<Map<String, Object>> banlistHits = detectInsecurePatterns(diffContent);
        results.put("banlistHits", banlistHits);
        
        // Run coverage analysis (placeholder for now)
        Map<String, Object> coverageResults = analyzeCoverage(diffAnalysis);
        results.put("coverage", coverageResults);
        
        logger.info("Pre-filter results: {} secrets, {} banlist hits", 
            secretHits.size(), banlistHits.size());
        
        return results;
    }
    
    private List<Map<String, Object>> detectSecrets(String diffContent) {
        List<Map<String, Object>> hits = new ArrayList<>();
        
        for (GatekeeperConfig.SecretRule rule : config.getSecretRules()) {
            if (rule.getPattern() != null) {
                hits.addAll(detectPatternSecrets(diffContent, rule));
            }
            if (rule.getEntropyBits() != null) {
                hits.addAll(detectHighEntropySecrets(diffContent, rule));
            }
        }
        
        return hits;
    }
    
    private List<Map<String, Object>> detectPatternSecrets(String diffContent, GatekeeperConfig.SecretRule rule) {
        List<Map<String, Object>> hits = new ArrayList<>();
        
        try {
            Pattern pattern = Pattern.compile(rule.getPattern());
            Matcher matcher = pattern.matcher(diffContent);
            
            while (matcher.find()) {
                Map<String, Object> hit = new HashMap<>();
                hit.put("rule", rule.getName());
                hit.put("pattern", rule.getPattern());
                hit.put("match", maskSecret(matcher.group()));
                hit.put("file", extractFileFromContext(diffContent, matcher.start()));
                hit.put("line", extractLineNumber(diffContent, matcher.start()));
                hits.add(hit);
            }
        } catch (Exception e) {
            logger.warn("Failed to compile pattern for rule {}: {}", rule.getName(), e.getMessage());
        }
        
        return hits;
    }
    
    private List<Map<String, Object>> detectHighEntropySecrets(String diffContent, GatekeeperConfig.SecretRule rule) {
        List<Map<String, Object>> hits = new ArrayList<>();
        
        // Simple entropy detection for strings that look like secrets
        Pattern stringPattern = Pattern.compile("\"([A-Za-z0-9+/=]{20,})\"");
        Matcher matcher = stringPattern.matcher(diffContent);
        
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (calculateEntropy(candidate) >= rule.getEntropyBits()) {
                Map<String, Object> hit = new HashMap<>();
                hit.put("rule", rule.getName());
                hit.put("pattern", "high_entropy");
                hit.put("match", maskSecret(candidate));
                hit.put("entropy", calculateEntropy(candidate));
                hit.put("file", extractFileFromContext(diffContent, matcher.start()));
                hit.put("line", extractLineNumber(diffContent, matcher.start()));
                hits.add(hit);
            }
        }
        
        return hits;
    }
    
    private List<Map<String, Object>> detectInsecurePatterns(String diffContent) {
        List<Map<String, Object>> hits = new ArrayList<>();
        
        for (String pattern : config.getInsecureBanlist()) {
            try {
                // Escape special regex characters and create pattern
                String escapedPattern = Pattern.quote(pattern);
                Pattern compiledPattern = Pattern.compile(escapedPattern);
                Matcher matcher = compiledPattern.matcher(diffContent);
                
                while (matcher.find()) {
                    Map<String, Object> hit = new HashMap<>();
                    hit.put("pattern", pattern);
                    hit.put("match", matcher.group());
                    hit.put("file", extractFileFromContext(diffContent, matcher.start()));
                    hit.put("line", extractLineNumber(diffContent, matcher.start()));
                    hits.add(hit);
                }
            } catch (Exception e) {
                logger.warn("Failed to compile banlist pattern {}: {}", pattern, e.getMessage());
            }
        }
        
        return hits;
    }
    
    private Map<String, Object> analyzeCoverage(Map<String, Object> diffAnalysis) {
        // Placeholder for coverage analysis
        // In a real implementation, this would parse coverage reports
        Map<String, Object> coverage = new HashMap<>();
        coverage.put("overall", 0.75); // Mock coverage
        coverage.put("changedFiles", new HashMap<>());
        return coverage;
    }
    
    private String maskSecret(String secret) {
        if (secret.length() <= 8) {
            return "*".repeat(secret.length());
        }
        return secret.substring(0, 4) + "*".repeat(secret.length() - 8) + secret.substring(secret.length() - 4);
    }
    
    private double calculateEntropy(String input) {
        Map<Character, Integer> frequencies = new HashMap<>();
        for (char c : input.toCharArray()) {
            frequencies.merge(c, 1, Integer::sum);
        }
        
        double entropy = 0.0;
        int length = input.length();
        
        for (int count : frequencies.values()) {
            double probability = (double) count / length;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        
        return entropy;
    }
    
    private String extractFileFromContext(String diffContent, int position) {
        // Find the most recent diff header before this position
        String beforePosition = diffContent.substring(0, position);
        String[] lines = beforePosition.split("\n");
        
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            if (line.startsWith("diff --git a/")) {
                String filePart = line.substring(13); // Remove "diff --git a/"
                int spaceIndex = filePart.indexOf(' ');
                if (spaceIndex > 0) {
                    return filePart.substring(0, spaceIndex);
                }
            }
        }
        
        return "unknown";
    }
    
    private int extractLineNumber(String diffContent, int position) {
        // Count lines from the start of the current hunk
        String beforePosition = diffContent.substring(0, position);
        String[] lines = beforePosition.split("\n");
        
        int lineNumber = 1;
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            if (line.startsWith("@@")) {
                // Extract line number from hunk header
                Pattern hunkPattern = Pattern.compile("@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");
                Matcher matcher = hunkPattern.matcher(line);
                if (matcher.find()) {
                    lineNumber = Integer.parseInt(matcher.group(1));
                }
                break;
            } else if (line.startsWith("+") || line.startsWith("-") || line.startsWith(" ")) {
                // Count context lines
                if (line.startsWith("+")) {
                    lineNumber++;
                }
            }
        }
        
        return lineNumber;
    }
}
