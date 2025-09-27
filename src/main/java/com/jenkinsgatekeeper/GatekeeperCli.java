package com.jenkinsgatekeeper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.jenkinsgatekeeper.service.GatekeeperService;
import com.jenkinsgatekeeper.config.GatekeeperConfig;
import com.jenkinsgatekeeper.model.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@Command(
    name = "ai-gatekeeper",
    description = "AI CI Gatekeeper for Jenkins - automatically review code changes for security and quality",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class GatekeeperCli implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(GatekeeperCli.class);
    
    @Option(
        names = {"--diff-file"},
        description = "Path to unified diff file to review",
        required = true
    )
    private File diffFile;
    
    @Option(
        names = {"--config"},
        description = "Path to configuration file (.aigate.yml)",
        defaultValue = ".aigate.yml"
    )
    private File configFile;
    
    @Option(
        names = {"--out"},
        description = "Output file for SARIF report (optional)"
    )
    private File outputFile;
    
    @Option(
        names = {"--format"},
        description = "Output format: console, sarif, both",
        defaultValue = "console"
    )
    private String format;
    
    @Option(
        names = {"--comment-pr"},
        description = "Whether to comment on PR (requires webhook configuration)",
        defaultValue = "false"
    )
    private boolean commentPr;
    
    @Option(
        names = {"--verbose"},
        description = "Enable verbose logging",
        defaultValue = "false"
    )
    private boolean verbose;
    
    @Option(
        names = {"--workspace"},
        description = "Workspace directory (defaults to current directory)"
    )
    private File workspace;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new GatekeeperCli()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        try {
            // Set workspace
            if (workspace == null) {
                workspace = new File(System.getProperty("user.dir"));
            }
            
            // Validate inputs
            if (!diffFile.exists()) {
                logger.error("Diff file does not exist: {}", diffFile.getAbsolutePath());
                return 1;
            }
            
            if (!configFile.exists()) {
                logger.error("Configuration file does not exist: {}", configFile.getAbsolutePath());
                return 1;
            }
            
            // Load configuration
            GatekeeperConfig config = GatekeeperConfig.load(configFile);
            logger.info("Loaded configuration from: {}", configFile.getAbsolutePath());
            
            // Initialize service
            GatekeeperService service = new GatekeeperService(config);
            
            // Read diff content
            String diffContent = Files.readString(diffFile.toPath());
            logger.info("Loaded diff file: {} ({} bytes)", diffFile.getName(), diffContent.length());
            
            // Perform review
            ReviewResult result = service.reviewDiff(diffContent, workspace.toPath());
            
            // Output results
            outputResults(result, format, outputFile);
            
            // Return appropriate exit code
            return switch (result.getVerdict()) {
                case PASS -> 0;
                case WARN -> 2;
                case FAIL -> 1;
            };
            
        } catch (Exception e) {
            logger.error("Gatekeeper execution failed", e);
            return 1;
        }
    }
    
    private void outputResults(ReviewResult result, String format, File outputFile) throws Exception {
        GatekeeperService service = new GatekeeperService(GatekeeperConfig.load(configFile));
        
        switch (format.toLowerCase()) {
            case "console" -> {
                System.out.println("\n=== AI Gatekeeper Review Results ===");
                System.out.printf("Risk Score: %d%n", result.getRiskScore());
                System.out.printf("Verdict: %s%n", result.getVerdict());
                System.out.printf("Findings: %d%n", result.getFindings().size());
                System.out.printf("Missing Tests: %d%n", result.getMissingTests().size());
                
                if (!result.getFindings().isEmpty()) {
                    System.out.println("\nFindings:");
                    result.getFindings().forEach(finding -> {
                        System.out.printf("  [%s] %s in %s:%d%n", 
                            finding.getSeverity(), finding.getTitle(), 
                            finding.getFile(), finding.getStartLine());
                        if (finding.getFixSuggestion() != null) {
                            System.out.printf("    Fix: %s%n", finding.getFixSuggestion());
                        }
                    });
                }
                
                if (!result.getMissingTests().isEmpty()) {
                    System.out.println("\nMissing Tests:");
                    result.getMissingTests().forEach(test -> {
                        System.out.printf("  %s -> %s%n", 
                            test.getFileUnderTest(), test.getSuggestedTestFile());
                    });
                }
            }
            
            case "sarif" -> {
                if (outputFile != null) {
                    // Ensure output directory exists
                    outputFile.getParentFile().mkdirs();
                    service.generateSarifReport(result, outputFile.toPath());
                    System.out.println("SARIF report written to: " + outputFile.getAbsolutePath());
                } else {
                    System.out.println("No output file specified for SARIF format");
                }
            }
            
            case "both" -> {
                // Output console first
                outputResults(result, "console", null);
                // Then SARIF
                if (outputFile != null) {
                    outputResults(result, "sarif", outputFile);
                }
            }
            
            default -> {
                logger.warn("Unknown output format: {}, using console", format);
                outputResults(result, "console", null);
            }
        }
    }
}
