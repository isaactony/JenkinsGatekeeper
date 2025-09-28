# AI CI Gatekeeper

AI-powered code review that automatically detects security vulnerabilities, secrets, and code quality issues in your CI/CD pipeline.

## Features

-  **Security Detection**: Secrets, vulnerabilities, insecure patterns
- **Code Quality**: Quality issues and improvements  
-  **Test Coverage**: Missing tests detection
- **Configurable**: YAML-based policies
-  **SARIF Reports**: Security dashboard integration
- **Multi-Platform**: Jenkins, GitHub Actions, CLI, Docker

## Usage Options

### 1. Jenkins Pipeline (Recommended)

**Drop-in integration for existing Jenkins projects:**

```groovy
// Add to your Jenkinsfile
pipeline {
    agent any
    environment {
        ANTHROPIC_API_KEY = credentials('ANTHROPIC_API_KEY')
    }
    stages {
        stage('AI Code Review') {
            steps {
                sh '''
                    # Generate diff
                    git diff --unified=0 HEAD~1...HEAD > changes.diff
                    
                    # Run AI review
                    java -jar ai-gatekeeper.jar \
                        --diff-file changes.diff \
                        --config .aigate.yml \
                        --format both
                '''
            }
        }
    }
}
```

### 2. GitHub Actions

**Automated PR reviews:**

```yaml
name: AI Code Review
on: [pull_request]

jobs:
  ai-review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      
      - name: Generate diff
        run: git diff --unified=0 origin/${{ github.base_ref }}...HEAD > changes.diff
      
      - name: AI Review
        run: |
          wget https://github.com/isaactony/JenkinsGatekeeper/releases/download/v1.0.0/ai-gatekeeper.jar
          java -jar ai-gatekeeper.jar \
            --diff-file changes.diff \
            --config .aigate.yml \
            --format both
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
```

### 3. Docker Container

**Isolated environment with all dependencies:**

```bash
# Download and load image
wget https://github.com/isaactony/JenkinsGatekeeper/releases/download/v1.0.0/ai-gatekeeper.tar.gz
gunzip -c ai-gatekeeper.tar.gz | docker load

# Run review
docker run --rm \
  -v $(pwd):/workspace \
  -w /workspace \
  -e ANTHROPIC_API_KEY="your-api-key" \
  ai-gatekeeper:latest
```

### 4. Command Line Interface

**Local development and testing:**

```bash
# Build from source
mvn clean package

# Generate diff
git diff --unified=0 HEAD~1...HEAD > changes.diff

# Run review
java -jar target/ai-gatekeeper-*.jar \
  --diff-file changes.diff \
  --config .aigate.yml \
  --format both \
  --verbose
```

### 5. Pre-commit Hook

**Catch issues before they're committed:**

```bash
#!/bin/sh
# .git/hooks/pre-commit

# Generate diff for staged changes
git diff --cached --unified=0 > /tmp/staged.diff

# Run AI review
java -jar ai-gatekeeper.jar \
  --diff-file /tmp/staged.diff \
  --config .aigate.yml

# Exit with review result
exit $?
```

## Configuration

Create `.aigate.yml` in your repository root:

```yaml
version: 1

# Risk thresholds (0-100)
fail_threshold: 70    # Fail build if risk score >= 70
warn_threshold: 40    # Warn if risk score >= 40

# Supported languages
languages: [java, javascript, python, go, rust, cpp, csharp]

# Test coverage requirements
require_tests_for_paths:
  - "src/main/**"
  - "lib/**"
  - "app/**"
coverage_min: 0.65    # Minimum 65% test coverage

# Secret detection rules
secret_rules:
  - name: AWS_ACCESS_KEY
    pattern: "AKIA[0-9A-Z]{16}"
  - name: AWS_SECRET_KEY
    pattern: "[A-Za-z0-9/+=]{40}"
  - name: API_KEY
    pattern: "(?i)(api[_-]?key|apikey)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{20,})['\"]?"
  - name: PASSWORD
    pattern: "(?i)(password|passwd|pwd)\\s*[:=]\\s*['\"]?([^\\s'\"]{8,})['\"]?"
  - name: GenericHighEntropy
    entropy_bits: 80  # Flag high-entropy strings

# Insecure patterns to flag
insecure_banlist:
  - "MessageDigest.getInstance(\"MD5\")"
  - "MessageDigest.getInstance(\"SHA1\")"
  - "eval("
  - "exec("
  - "Runtime.exec"
  - "ProcessBuilder"
  - "System.setProperty"
  - "FileInputStream"
  - "FileOutputStream"
  - "SELECT.*FROM.*WHERE.*="  # SQL injection patterns
  - "document.write"
  - "innerHTML\\s*="

# LLM configuration
model: "claude-sonnet-4-20250514"
max_tokens: 2000
temperature: 0.1      # Low temperature for consistent results
timeout_seconds: 300  # 5 minute timeout
max_retries: 2

# Output options
output:
  console: true
  sarif: true
  pr_comments: false  # Set to true for GitHub PR comments
```

## Advanced Usage

### Custom Secret Patterns

```yaml
secret_rules:
  - name: CUSTOM_API_KEY
    pattern: "myapp_[a-zA-Z0-9]{32}"
  - name: DATABASE_URL
    pattern: "postgresql://[^:]+:[^@]+@[^/]+/[^?]+"
```

### Language-Specific Rules

```yaml
# Java-specific insecure patterns
insecure_banlist:
  - "java.security.MessageDigest.getInstance"
  - "javax.crypto.Cipher.getInstance"
  - "java.sql.Statement.executeQuery"

# JavaScript-specific patterns  
insecure_banlist:
  - "eval("
  - "Function("
  - "setTimeout(.*,.*)"
  - "setInterval(.*,.*)"
```

### Coverage Exclusions

```yaml
coverage_exclusions:
  - "**/test/**"
  - "**/tests/**"
  - "**/*Test.java"
  - "**/*Spec.java"
```

## Exit Codes

- `0`: **PASS** - No issues found or within acceptable thresholds
- `2`: **WARN** - Issues found but within warning thresholds  
- `1`: **FAIL** - Critical issues found or exceeding fail thresholds

## Requirements

- **Java 21+** or Docker
- **Anthropic API key** (get from [console.anthropic.com](https://console.anthropic.com))
- **Git repository** with changes to review

