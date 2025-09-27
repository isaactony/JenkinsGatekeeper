package com.jenkinsgatekeeper.service.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jenkinsgatekeeper.model.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class ReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    
    private final ObjectMapper objectMapper;
    
    public ReportGenerator() {
        this.objectMapper = new ObjectMapper();
    }
    
    public void generateSarifReport(ReviewResult result, Path outputPath) throws IOException {
        logger.info("Generating SARIF report to: {}", outputPath);
        
        // Ensure output directory exists
        Files.createDirectories(outputPath.getParent());
        
        ObjectNode sarif = objectMapper.createObjectNode();
        sarif.put("$schema", "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json");
        sarif.put("version", "2.1.0");
        
        ArrayNode runs = objectMapper.createArrayNode();
        ObjectNode run = objectMapper.createObjectNode();
        
        // Tool information
        ObjectNode tool = objectMapper.createObjectNode();
        ObjectNode driver = objectMapper.createObjectNode();
        driver.put("name", "AI CI Gatekeeper");
        driver.put("version", "1.0.0");
        driver.put("informationUri", "https://github.com/your-org/ai-gatekeeper");
        
        ObjectNode rules = objectMapper.createObjectNode();
        addRule(rules, "SEC-001", "Secret Detection", "Secrets detected in code changes");
        addRule(rules, "SEC-002", "Insecure Pattern", "Insecure coding patterns detected");
        addRule(rules, "QUAL-001", "Code Quality", "Code quality issues identified");
        addRule(rules, "TEST-001", "Missing Tests", "Missing test coverage for changed code");
        
        driver.set("rules", rules);
        tool.set("driver", driver);
        run.set("tool", tool);
        
        // Results
        ArrayNode results = objectMapper.createArrayNode();
        for (ReviewResult.Finding finding : result.getFindings()) {
            ObjectNode sarifResult = objectMapper.createObjectNode();
            sarifResult.put("ruleId", finding.getId());
            sarifResult.put("level", mapSeverityToSarifLevel(finding.getSeverity()));
            sarifResult.set("message", objectMapper.createObjectNode().put("text", finding.getTitle()));
            
            // Location
            ObjectNode location = objectMapper.createObjectNode();
            ObjectNode physicalLocation = objectMapper.createObjectNode();
            ObjectNode artifactLocation = objectMapper.createObjectNode();
            artifactLocation.put("uri", finding.getFile());
            physicalLocation.set("artifactLocation", artifactLocation);
            
            ObjectNode region = objectMapper.createObjectNode();
            region.put("startLine", finding.getStartLine());
            region.put("endLine", finding.getEndLine());
            physicalLocation.set("region", region);
            
            location.set("physicalLocation", physicalLocation);
            ArrayNode locations = objectMapper.createArrayNode();
            locations.add(location);
            sarifResult.set("locations", locations);
            
            // Properties
            ObjectNode properties = objectMapper.createObjectNode();
            if (finding.getFixSuggestion() != null) {
                properties.put("fixSuggestion", finding.getFixSuggestion());
            }
            if (finding.getPatch() != null) {
                properties.put("patch", finding.getPatch());
            }
            sarifResult.set("properties", properties);
            
            results.add(sarifResult);
        }
        
        run.set("results", results);
        
        // Invocation
        ObjectNode invocation = objectMapper.createObjectNode();
        invocation.put("startTimeUtc", Instant.now().toString());
        invocation.put("endTimeUtc", Instant.now().toString());
        invocation.put("exitCode", mapVerdictToExitCode(result.getVerdict()));
        run.set("invocations", objectMapper.createArrayNode().add(invocation));
        
        // Properties
        ObjectNode runProperties = objectMapper.createObjectNode();
        runProperties.put("riskScore", result.getRiskScore());
        runProperties.put("verdict", result.getVerdict().toString());
        runProperties.put("findingsCount", result.getFindings().size());
        runProperties.put("missingTestsCount", result.getMissingTests().size());
        run.set("properties", runProperties);
        
        runs.add(run);
        sarif.set("runs", runs);
        
        // Write to file
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), sarif);
        logger.info("SARIF report written successfully");
    }
    
    private void addRule(ObjectNode rules, String id, String name, String description) {
        ObjectNode rule = objectMapper.createObjectNode();
        rule.put("id", id);
        rule.put("name", name);
        rule.set("shortDescription", objectMapper.createObjectNode().put("text", description));
        rule.set("fullDescription", objectMapper.createObjectNode().put("text", description));
        rule.set("defaultConfiguration", objectMapper.createObjectNode().put("level", "warning"));
        rules.set(id, rule);
    }
    
    private String mapSeverityToSarifLevel(ReviewResult.Finding.Severity severity) {
        return switch (severity) {
            case INFO, LOW -> "note";
            case MEDIUM -> "warning";
            case HIGH, CRITICAL -> "error";
        };
    }
    
    private int mapVerdictToExitCode(ReviewResult.Verdict verdict) {
        return switch (verdict) {
            case PASS -> 0;
            case WARN -> 2;
            case FAIL -> 1;
        };
    }
}
