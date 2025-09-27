package com.jenkinsgatekeeper.service.policy;

import com.jenkinsgatekeeper.config.GatekeeperConfig;
import com.jenkinsgatekeeper.model.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyEngineTest {
    
    private PolicyEngine policyEngine;
    
    @BeforeEach
    void setUp() {
        GatekeeperConfig config = new GatekeeperConfig();
        config.setFailThreshold(70);
        config.setWarnThreshold(40);
        this.policyEngine = new PolicyEngine(config);
    }
    
    @Test
    void testPassVerdict() {
        ReviewResult result = new ReviewResult(30, ReviewResult.Verdict.PASS);
        result.setFindings(List.of());
        
        Map<String, Object> preFilterResults = Map.of(
            "secretHits", List.of(),
            "banlistHits", List.of()
        );
        
        ReviewResult evaluated = policyEngine.evaluateResult(result, preFilterResults);
        
        assertThat(evaluated.getVerdict()).isEqualTo(ReviewResult.Verdict.PASS);
        assertThat(evaluated.getRiskScore()).isEqualTo(30);
    }
    
    @Test
    void testWarnVerdict() {
        ReviewResult result = new ReviewResult(50, ReviewResult.Verdict.PASS);
        result.setFindings(List.of());
        
        Map<String, Object> preFilterResults = Map.of(
            "secretHits", List.of(),
            "banlistHits", List.of()
        );
        
        ReviewResult evaluated = policyEngine.evaluateResult(result, preFilterResults);
        
        assertThat(evaluated.getVerdict()).isEqualTo(ReviewResult.Verdict.WARN);
        assertThat(evaluated.getRiskScore()).isEqualTo(50);
    }
    
    @Test
    void testFailVerdict() {
        ReviewResult result = new ReviewResult(80, ReviewResult.Verdict.PASS);
        result.setFindings(List.of());
        
        Map<String, Object> preFilterResults = Map.of(
            "secretHits", List.of(),
            "banlistHits", List.of()
        );
        
        ReviewResult evaluated = policyEngine.evaluateResult(result, preFilterResults);
        
        assertThat(evaluated.getVerdict()).isEqualTo(ReviewResult.Verdict.FAIL);
        assertThat(evaluated.getRiskScore()).isEqualTo(80);
    }
    
    @Test
    void testSecretHitAlwaysFails() {
        ReviewResult result = new ReviewResult(0, ReviewResult.Verdict.PASS);
        result.setFindings(List.of());
        
        Map<String, Object> secretHit = Map.of(
            "rule", "AWS_ACCESS_KEY",
            "file", "src/main/java/Example.java",
            "line", 5
        );
        
        Map<String, Object> preFilterResults = Map.of(
            "secretHits", List.of(secretHit),
            "banlistHits", List.of()
        );
        
        ReviewResult evaluated = policyEngine.evaluateResult(result, preFilterResults);
        
        assertThat(evaluated.getVerdict()).isEqualTo(ReviewResult.Verdict.FAIL);
        assertThat(evaluated.getRiskScore()).isEqualTo(100);
    }
    
    @Test
    void testHighSeverityFindingFails() {
        ReviewResult.Finding finding = new ReviewResult.Finding(
            "SEC-001", "Critical security issue", 
            ReviewResult.Finding.Severity.CRITICAL, 
            "src/main/java/Example.java", 10
        );
        
        ReviewResult result = new ReviewResult(0, ReviewResult.Verdict.PASS);
        result.setFindings(List.of(finding));
        
        Map<String, Object> preFilterResults = Map.of(
            "secretHits", List.of(),
            "banlistHits", List.of()
        );
        
        ReviewResult evaluated = policyEngine.evaluateResult(result, preFilterResults);
        
        assertThat(evaluated.getVerdict()).isEqualTo(ReviewResult.Verdict.FAIL);
        assertThat(evaluated.getRiskScore()).isEqualTo(100);
    }
    
    @Test
    void testShouldRequireTests() {
        assertThat(policyEngine.shouldRequireTests("src/main/java/Example.java")).isTrue();
        assertThat(policyEngine.shouldRequireTests("src/test/java/ExampleTest.java")).isFalse();
        assertThat(policyEngine.shouldRequireTests("docs/README.md")).isFalse();
    }
}
