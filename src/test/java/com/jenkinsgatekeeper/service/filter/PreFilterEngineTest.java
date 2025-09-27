package com.jenkinsgatekeeper.service.filter;

import com.jenkinsgatekeeper.config.GatekeeperConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PreFilterEngineTest {
    
    private PreFilterEngine preFilterEngine;
    
    @BeforeEach
    void setUp() {
        GatekeeperConfig config = new GatekeeperConfig();
        this.preFilterEngine = new PreFilterEngine(config);
    }
    
    @Test
    void testSecretDetection() {
        String diffContent = """
            diff --git a/src/main/java/Example.java b/src/main/java/Example.java
            index 1234567..abcdefg 100644
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,3 +1,4 @@
             public class Example {
             +    private String awsKey = "AKIA1234567890ABCDEF";
                 public void method() {
             }
            """;
        
        Map<String, Object> diffAnalysis = Map.of(
            "changedFiles", List.of("src/main/java/Example.java"),
            "content", diffContent
        );
        
        Map<String, Object> results = preFilterEngine.runFilters(diffContent, diffAnalysis);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secretHits = (List<Map<String, Object>>) results.get("secretHits");
        
        assertThat(secretHits).isNotEmpty();
        assertThat(secretHits.get(0).get("rule")).isEqualTo("AWS_ACCESS_KEY");
        assertThat(secretHits.get(0).get("file")).isEqualTo("src/main/java/Example.java");
    }
    
    @Test
    void testInsecurePatternDetection() {
        String diffContent = """
            diff --git a/src/main/java/Example.java b/src/main/java/Example.java
            index 1234567..abcdefg 100644
            --- a/src/main/java/Example.java
            +++ b/src/main/java/Example.java
            @@ -1,3 +1,4 @@
             public class Example {
             +    MessageDigest md = MessageDigest.getInstance("MD5");
                 public void method() {
             }
            """;
        
        Map<String, Object> diffAnalysis = Map.of(
            "changedFiles", List.of("src/main/java/Example.java"),
            "content", diffContent
        );
        
        Map<String, Object> results = preFilterEngine.runFilters(diffContent, diffAnalysis);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> banlistHits = (List<Map<String, Object>>) results.get("banlistHits");
        
        assertThat(banlistHits).isNotEmpty();
        assertThat(banlistHits.get(0).get("pattern")).isEqualTo("MessageDigest.getInstance(\"MD5\")");
        assertThat(banlistHits.get(0).get("file")).isEqualTo("src/main/java/Example.java");
    }
    
    @Test
    void testNoIssuesFound() {
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
        
        Map<String, Object> diffAnalysis = Map.of(
            "changedFiles", List.of("src/main/java/Example.java"),
            "content", diffContent
        );
        
        Map<String, Object> results = preFilterEngine.runFilters(diffContent, diffAnalysis);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secretHits = (List<Map<String, Object>>) results.get("secretHits");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> banlistHits = (List<Map<String, Object>>) results.get("banlistHits");
        
        assertThat(secretHits).isEmpty();
        assertThat(banlistHits).isEmpty();
    }
}
