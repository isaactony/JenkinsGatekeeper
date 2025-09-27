# AI CI Gatekeeper

AI-powered code review for Jenkins that detects security vulnerabilities, secrets, and code quality issues.

## Features

-  **Security Detection**: Secrets, vulnerabilities, insecure patterns
-  **Code Quality**: Quality issues and improvements
-  **Test Coverage**: Missing tests detection
-  **Configurable**: YAML-based policies
-  **SARIF Reports**: Security dashboard integration
-  **Jenkins Ready**: Drop-in pipeline integration

## Quick Start

### Docker (Recommended)

```bash
# Download from GitHub Releases
wget https://github.com/isaactony/JenkinsGatekeeper/releases/download/v1.0.0/ai-gatekeeper.tar.gz

# Load and run
gunzip -c ai-gatekeeper.tar.gz | docker load
docker run --rm \
  -v $(pwd):/workspace \
  -w /workspace \
  -e ANTHROPIC_API_KEY="your-api-key" \
  ai-gatekeeper:latest
```

### Jenkins Integration

1. Add `Jenkinsfile` to your repo
2. Set `ANTHROPIC_API_KEY` in Jenkins credentials
3. Pipeline runs automatically on PRs

### Configuration

Create `.aigate.yml`:

```yaml
version: 1
fail_threshold: 70
warn_threshold: 40
secret_rules:
  - name: AWS_ACCESS_KEY
    pattern: "AKIA[0-9A-Z]{16}"
insecure_banlist:
  - "MessageDigest.getInstance(\"MD5\")"
model: "claude-sonnet-4-20250514"
```

## Usage

```bash
# CLI
java -jar ai-gatekeeper.jar \
  --diff-file changes.diff \
  --config .aigate.yml \
  --format both

# Exit codes: 0=PASS, 2=WARN, 1=FAIL
```

## Requirements

- Java 21+ or Docker
- Anthropic API key
- Git repository

