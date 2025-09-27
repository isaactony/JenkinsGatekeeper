package com.jenkinsgatekeeper.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GatekeeperConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(GatekeeperConfig.class);
    
    @JsonProperty("version")
    private int version = 1;
    
    @JsonProperty("fail_threshold")
    private int failThreshold = 70;
    
    @JsonProperty("warn_threshold")
    private int warnThreshold = 40;
    
    @JsonProperty("languages")
    private List<String> languages = List.of("java", "js", "py");
    
    @JsonProperty("require_tests_for_paths")
    private List<String> requireTestsForPaths = List.of("src/main/**");
    
    @JsonProperty("coverage_min")
    private double coverageMin = 0.65;
    
    @JsonProperty("secret_rules")
    private List<SecretRule> secretRules;
    
    @JsonProperty("insecure_banlist")
    private List<String> insecureBanlist;
    
    @JsonProperty("model")
    private String model = "claude-3-5-sonnet-20241022";
    
    @JsonProperty("max_tokens")
    private int maxTokens = 2000;
    
    @JsonProperty("temperature")
    private double temperature = 0.1;
    
    @JsonProperty("timeout_seconds")
    private int timeoutSeconds = 300;
    
    @JsonProperty("max_retries")
    private int maxRetries = 2;
    
    // Default constructor
    public GatekeeperConfig() {
        this.secretRules = List.of(
            new SecretRule("AWS_ACCESS_KEY", "AKIA[0-9A-Z]{16}"),
            new SecretRule("AWS_SECRET_KEY", "[A-Za-z0-9/+=]{40}"),
            new SecretRule("GenericHighEntropy", null, 80)
        );
        
        this.insecureBanlist = List.of(
            "MessageDigest.getInstance(\"MD5\")",
            "eval(",
            "exec(",
            "Runtime.getRuntime().exec(",
            "ProcessBuilder(",
            "System.setProperty(",
            "System.getenv("
        );
    }
    
    public static GatekeeperConfig load(File configFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        GatekeeperConfig config = mapper.readValue(configFile, GatekeeperConfig.class);
        logger.info("Loaded configuration: version={}, fail_threshold={}, warn_threshold={}", 
            config.version, config.failThreshold, config.warnThreshold);
        return config;
    }
    
    // Getters
    public int getVersion() { return version; }
    public int getFailThreshold() { return failThreshold; }
    public int getWarnThreshold() { return warnThreshold; }
    public List<String> getLanguages() { return languages; }
    public List<String> getRequireTestsForPaths() { return requireTestsForPaths; }
    public double getCoverageMin() { return coverageMin; }
    public List<SecretRule> getSecretRules() { return secretRules; }
    public List<String> getInsecureBanlist() { return insecureBanlist; }
    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getMaxRetries() { return maxRetries; }
    
    // Setters
    public void setFailThreshold(int failThreshold) { this.failThreshold = failThreshold; }
    public void setWarnThreshold(int warnThreshold) { this.warnThreshold = warnThreshold; }
    
    public static class SecretRule {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("pattern")
        private String pattern;
        
        @JsonProperty("entropy_bits")
        private Integer entropyBits;
        
        public SecretRule() {}
        
        public SecretRule(String name, String pattern) {
            this.name = name;
            this.pattern = pattern;
        }
        
        public SecretRule(String name, String pattern, Integer entropyBits) {
            this.name = name;
            this.pattern = pattern;
            this.entropyBits = entropyBits;
        }
        
        // Getters
        public String getName() { return name; }
        public String getPattern() { return pattern; }
        public Integer getEntropyBits() { return entropyBits; }
    }
}
