package com.jenkinsgatekeeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

public class ReviewResult {
    
    @JsonProperty("risk_score")
    private int riskScore;
    
    @JsonProperty("verdict")
    private Verdict verdict;
    
    @JsonProperty("findings")
    private List<Finding> findings = new ArrayList<>();
    
    @JsonProperty("missing_tests")
    private List<MissingTest> missingTests = new ArrayList<>();
    
    public ReviewResult() {}
    
    public ReviewResult(int riskScore, Verdict verdict) {
        this.riskScore = riskScore;
        this.verdict = verdict;
    }
    
    // Getters and Setters
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    
    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }
    
    public List<Finding> getFindings() { return findings; }
    public void setFindings(List<Finding> findings) { this.findings = findings; }
    
    public List<MissingTest> getMissingTests() { return missingTests; }
    public void setMissingTests(List<MissingTest> missingTests) { this.missingTests = missingTests; }
    
    public enum Verdict {
        PASS, WARN, FAIL
    }
    
    public static class Finding {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("severity")
        private Severity severity;
        
        @JsonProperty("file")
        private String file;
        
        @JsonProperty("start_line")
        private int startLine;
        
        @JsonProperty("end_line")
        private int endLine;
        
        @JsonProperty("rationale")
        private String rationale;
        
        @JsonProperty("fix_suggestion")
        private String fixSuggestion;
        
        @JsonProperty("patch")
        private String patch;
        
        public Finding() {}
        
        public Finding(String id, String title, Severity severity, String file, int startLine) {
            this.id = id;
            this.title = title;
            this.severity = severity;
            this.file = file;
            this.startLine = startLine;
            this.endLine = startLine;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }
        
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        
        public int getStartLine() { return startLine; }
        public void setStartLine(int startLine) { this.startLine = startLine; }
        
        public int getEndLine() { return endLine; }
        public void setEndLine(int endLine) { this.endLine = endLine; }
        
        public String getRationale() { return rationale; }
        public void setRationale(String rationale) { this.rationale = rationale; }
        
        public String getFixSuggestion() { return fixSuggestion; }
        public void setFixSuggestion(String fixSuggestion) { this.fixSuggestion = fixSuggestion; }
        
        public String getPatch() { return patch; }
        public void setPatch(String patch) { this.patch = patch; }
        
        public enum Severity {
            INFO(10), LOW(30), MEDIUM(60), HIGH(80), CRITICAL(100);
            
            private final int score;
            
            Severity(int score) {
                this.score = score;
            }
            
            public int getScore() { return score; }
        }
    }
    
    public static class MissingTest {
        @JsonProperty("file_under_test")
        private String fileUnderTest;
        
        @JsonProperty("suggested_test_file")
        private String suggestedTestFile;
        
        @JsonProperty("test_stub")
        private String testStub;
        
        public MissingTest() {}
        
        public MissingTest(String fileUnderTest, String suggestedTestFile) {
            this.fileUnderTest = fileUnderTest;
            this.suggestedTestFile = suggestedTestFile;
        }
        
        // Getters and Setters
        public String getFileUnderTest() { return fileUnderTest; }
        public void setFileUnderTest(String fileUnderTest) { this.fileUnderTest = fileUnderTest; }
        
        public String getSuggestedTestFile() { return suggestedTestFile; }
        public void setSuggestedTestFile(String suggestedTestFile) { this.suggestedTestFile = suggestedTestFile; }
        
        public String getTestStub() { return testStub; }
        public void setTestStub(String testStub) { this.testStub = testStub; }
    }
}
