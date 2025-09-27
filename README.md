# AI CI Gatekeeper

A production-ready AI-powered CI gatekeeper for Jenkins that automatically reviews code changes for security vulnerabilities, insecure patterns, and test coverage gaps. The gatekeeper generates actionable fixes, annotates builds, and blocks merges based on configurable policies.

## Features

- **Security Detection**: Automatically detects secrets, insecure APIs, and security vulnerabilities
- **Code Quality**: Identifies code quality issues and suggests improvements
- **Test Coverage**: Ensures adequate test coverage for changed code
- **Configurable Policies**: YAML-based configuration for thresholds, banlists, and rules
- **Multiple Output Formats**: Console output, SARIF reports, and optional PR comments
- **Jenkins Integration**: Seamless integration with Jenkins pipelines
- **Deterministic Results**: Low temperature settings for consistent CI behavior

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Jenkins with Java 21 agents
- Anthropic API key

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd JenkinsGatekeeper
```

2. Build the application:
```bash
mvn clean package
```

3. Configure your repository with a `.aigate.yml` file (see Configuration section)

4. Set up Jenkins credentials for your Anthropic API key

### Jenkins Integration

1. Copy the provided `Jenkinsfile` to your repository
2. Configure Jenkins credentials for `ANTHROPIC_API_KEY`
3. Adjust the agent label in the Jenkinsfile to match your Jenkins setup
4. The pipeline will automatically run on PR builds and branch pushes

## Configuration

Create a `.aigate.yml` file in your repository root:

```yaml
version: 1
fail_threshold: 70
warn_threshold: 40
languages: [java, js, py]
require_tests_for_paths:
  - "src/main/**"
coverage_min: 0.65
secret_rules:
  - name: AWS_ACCESS_KEY
    pattern: "AKIA[0-9A-Z]{16}"
  - name: GenericHighEntropy
    entropy_bits: 80
insecure_banlist:
  - "MessageDigest.getInstance(\"MD5\")"
  - "eval("
model: "claude-3-5-sonnet-20241022"
max_tokens: 2000
temperature: 0.1
timeout_seconds: 300
max_retries: 2
```

### Configuration Options

- `fail_threshold`: Risk score threshold for failing builds (0-100)
- `warn_threshold`: Risk score threshold for warnings (0-100)
- `languages`: Supported programming languages
- `require_tests_for_paths`: Path patterns that require test coverage
- `coverage_min`: Minimum test coverage percentage
- `secret_rules`: Custom secret detection patterns
- `insecure_banlist`: Patterns to flag as insecure
- `model`: Anthropic model to use
- `max_tokens`: Maximum tokens for LLM responses
- `temperature`: LLM temperature (0.0-1.0, lower for deterministic results)
- `timeout_seconds`: API timeout in seconds
- `max_retries`: Maximum retry attempts for API calls

## Usage

### Command Line Interface

```bash
# Basic usage
java -jar target/ai-gatekeeper-*.jar \
  --diff-file gatekeeper.diff \
  --config .aigate.yml

# With SARIF output
java -jar target/ai-gatekeeper-*.jar \
  --diff-file gatekeeper.diff \
  --config .aigate.yml \
  --format sarif \
  --out build/aigate.sarif

# Verbose output
java -jar target/ai-gatekeeper-*.jar \
  --diff-file gatekeeper.diff \
  --config .aigate.yml \
  --verbose
```

### CLI Options

- `--diff-file`: Path to unified diff file (required)
- `--config`: Path to configuration file (default: `.aigate.yml`)
- `--out`: Output file for SARIF report
- `--format`: Output format (console, sarif, both)
- `--comment-pr`: Whether to comment on PR (requires webhook)
- `--verbose`: Enable verbose logging
- `--workspace`: Workspace directory

### Exit Codes

- `0`: PASS - No issues found or within acceptable thresholds
- `2`: WARN - Issues found but within warning thresholds
- `1`: FAIL - Critical issues found or exceeding fail thresholds

## How It Works

### 1. Diff Collection
The gatekeeper analyzes Git diffs to understand what code has changed. It supports:
- PR builds (comparing against target branch)
- Branch builds (comparing against main branch)
- Release builds (comparing against previous release)

### 2. Pre-Filtering
Fast pre-filters run before LLM analysis:
- **Secret Detection**: Regex patterns and entropy analysis
- **Insecure Patterns**: Banlist matching for known insecure APIs
- **Coverage Analysis**: Test coverage validation

### 3. LLM Review
The Anthropic Claude model analyzes the code changes for:
- Security vulnerabilities
- Code quality issues
- Missing tests
- Performance concerns

### 4. Policy Evaluation
The policy engine applies configurable rules:
- Risk score calculation
- Severity mapping
- Verdict determination (PASS/WARN/FAIL)

### 5. Reporting
Results are output in multiple formats:
- Console summary
- SARIF reports for security dashboards
- Optional PR comments

## Security Considerations

- API keys are stored in Jenkins credentials and never logged
- Secrets are redacted in prompts while preserving context
- No PII is logged; only file paths and line numbers
- Supports self-hosted agents with restricted network access

## Troubleshooting

### Common Issues

1. **API Key Not Found**
   - Ensure `ANTHROPIC_API_KEY` is set in Jenkins credentials
   - Verify the credential ID matches the Jenkinsfile

2. **Diff File Not Found**
   - Check that the diff computation stage completed successfully
   - Verify Git operations in the Jenkins pipeline

3. **High False Positives**
   - Adjust thresholds in `.aigate.yml`
   - Add exceptions to banlist patterns
   - Review and tune secret detection rules

4. **Timeout Issues**
   - Increase `timeout_seconds` in configuration
   - Consider chunking large diffs
   - Check network connectivity to Anthropic API

### Debug Mode

Enable verbose logging to see detailed information:

```bash
java -jar target/ai-gatekeeper-*.jar \
  --diff-file gatekeeper.diff \
  --config .aigate.yml \
  --verbose
```

## Development

### Building from Source

```bash
# Clone repository
git clone <repository-url>
cd JenkinsGatekeeper

# Build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=GatekeeperServiceTest
```

### Project Structure

```
src/
├── main/java/com/jenkinsgatekeeper/
│   ├── GatekeeperCli.java          # CLI entry point
│   ├── config/
│   │   └── GatekeeperConfig.java   # Configuration model
│   ├── model/
│   │   └── ReviewResult.java       # Result models
│   └── service/
│       ├── GatekeeperService.java  # Main service
│       ├── diff/
│       │   └── DiffCollector.java  # Git diff analysis
│       ├── filter/
│       │   └── PreFilterEngine.java # Pre-filtering
│       ├── llm/
│       │   └── AnthropicClient.java # LLM integration
│       ├── policy/
│       │   └── PolicyEngine.java   # Policy evaluation
│       └── report/
│           └── ReportGenerator.java # Report generation
└── test/java/                      # Test classes
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review existing GitHub issues
3. Create a new issue with detailed information
4. Include relevant logs and configuration
