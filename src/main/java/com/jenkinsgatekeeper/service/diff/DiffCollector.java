package com.jenkinsgatekeeper.service.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiffCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(DiffCollector.class);
    
    // Pattern to match diff headers
    private static final Pattern DIFF_HEADER_PATTERN = Pattern.compile("^diff --git a/(.+) b/(.+)$", Pattern.MULTILINE);
    
    public Map<String, Object> analyzeDiff(String diffContent) {
        logger.info("Analyzing diff content ({} bytes)", diffContent.length());
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Extract changed files
        List<String> changedFiles = extractChangedFiles(diffContent);
        analysis.put("changedFiles", changedFiles);
        
        // Extract file statistics
        Map<String, FileStats> fileStats = extractFileStats(diffContent);
        analysis.put("fileStats", fileStats);
        
        // Store original content
        analysis.put("content", diffContent);
        
        // Calculate overall statistics
        int totalAdditions = fileStats.values().stream().mapToInt(FileStats::getAdditions).sum();
        int totalDeletions = fileStats.values().stream().mapToInt(FileStats::getDeletions).sum();
        
        analysis.put("totalAdditions", totalAdditions);
        analysis.put("totalDeletions", totalDeletions);
        analysis.put("totalChanges", totalAdditions + totalDeletions);
        
        logger.info("Diff analysis: {} files changed, {} additions, {} deletions", 
            changedFiles.size(), totalAdditions, totalDeletions);
        
        return analysis;
    }
    
    private List<String> extractChangedFiles(String diffContent) {
        List<String> files = new ArrayList<>();
        Matcher matcher = DIFF_HEADER_PATTERN.matcher(diffContent);
        
        while (matcher.find()) {
            String filePath = matcher.group(1);
            files.add(filePath);
        }
        
        return files;
    }
    
    private Map<String, FileStats> extractFileStats(String diffContent) {
        Map<String, FileStats> stats = new HashMap<>();
        
        // Split diff by file headers
        String[] fileDiffs = diffContent.split("^diff --git", Pattern.MULTILINE);
        
        for (String fileDiff : fileDiffs) {
            if (fileDiff.trim().isEmpty()) continue;
            
            // Extract file path from the first line
            String[] lines = fileDiff.split("\n");
            if (lines.length == 0) continue;
            
            String firstLine = lines[0].trim();
            if (!firstLine.startsWith("a/")) continue;
            
            String filePath = firstLine.substring(2); // Remove "a/" prefix
            
            // Count additions and deletions
            int additions = 0;
            int deletions = 0;
            
            for (String line : lines) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    additions++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    deletions++;
                }
            }
            
            stats.put(filePath, new FileStats(additions, deletions));
        }
        
        return stats;
    }
    
    public static class FileStats {
        private final int additions;
        private final int deletions;
        
        public FileStats(int additions, int deletions) {
            this.additions = additions;
            this.deletions = deletions;
        }
        
        public int getAdditions() { return additions; }
        public int getDeletions() { return deletions; }
        public int getTotalChanges() { return additions + deletions; }
    }
}
