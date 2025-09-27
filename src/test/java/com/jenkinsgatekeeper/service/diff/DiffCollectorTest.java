package com.jenkinsgatekeeper.service.diff;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DiffCollectorTest {
    
    private final DiffCollector diffCollector = new DiffCollector();
    
    @Test
    void testAnalyzeDiff() {
        String diffContent = """
            diff --git a/src/main/java/Example.java b/src/main/java/Example.java
            index 1234567..abcdefg 100644
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,3 +1,4 @@
             public class Example {
             +    private String field = "value";
                 public void method() {
             }
            """;
        
        Map<String, Object> analysis = diffCollector.analyzeDiff(diffContent);
        
        assertThat(analysis).containsKey("changedFiles");
        assertThat(analysis).containsKey("fileStats");
        assertThat(analysis).containsKey("content");
        assertThat(analysis).containsKey("totalAdditions");
        assertThat(analysis).containsKey("totalDeletions");
        assertThat(analysis).containsKey("totalChanges");
        
        @SuppressWarnings("unchecked")
        List<String> changedFiles = (List<String>) analysis.get("changedFiles");
        assertThat(changedFiles).hasSize(1);
        assertThat(changedFiles.get(0)).isEqualTo("src/main/java/Example.java");
        
        Integer totalAdditions = (Integer) analysis.get("totalAdditions");
        assertThat(totalAdditions).isEqualTo(1);
        
        Integer totalDeletions = (Integer) analysis.get("totalDeletions");
        assertThat(totalDeletions).isEqualTo(0);
    }
    
    @Test
    void testAnalyzeEmptyDiff() {
        String diffContent = "";
        
        Map<String, Object> analysis = diffCollector.analyzeDiff(diffContent);
        
        @SuppressWarnings("unchecked")
        List<String> changedFiles = (List<String>) analysis.get("changedFiles");
        assertThat(changedFiles).isEmpty();
        
        Integer totalAdditions = (Integer) analysis.get("totalAdditions");
        assertThat(totalAdditions).isEqualTo(0);
    }
    
    @Test
    void testAnalyzeMultipleFiles() {
        String diffContent = """
            diff --git a/src/main/java/Example1.java b/src/main/java/Example1.java
            index 1234567..abcdefg 100644
            --- a/src/main/java/Example1.java
            +++ b/src/main/java/Example1.java
            @@ -1,3 +1,4 @@
             public class Example1 {
             +    private String field1 = "value1";
                 public void method() {
             }
            diff --git a/src/main/java/Example2.java b/src/main/java/Example2.java
            index 1234567..abcdefg 100644
            --- a/src/main/java/Example2.java
            +++ b/src/main/java/Example2.java
            @@ -1,3 +1,4 @@
             public class Example2 {
             +    private String field2 = "value2";
                 public void method() {
             }
            """;
        
        Map<String, Object> analysis = diffCollector.analyzeDiff(diffContent);
        
        @SuppressWarnings("unchecked")
        List<String> changedFiles = (List<String>) analysis.get("changedFiles");
        assertThat(changedFiles).hasSize(2);
        assertThat(changedFiles).contains("src/main/java/Example1.java", "src/main/java/Example2.java");
        
        Integer totalAdditions = (Integer) analysis.get("totalAdditions");
        assertThat(totalAdditions).isEqualTo(2);
    }
}
