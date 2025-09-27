FROM eclipse-temurin:21-jdk

# Install git for diff generation
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the AI Gatekeeper JAR
COPY target/ai-gatekeeper-*.jar /app/ai-gatekeeper.jar

# Copy default configuration
COPY .aigate.yml /app/.aigate.yml

# Create entrypoint script
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
# Generate diff if not provided\n\
if [ ! -f "$DIFF_FILE" ]; then\n\
    echo "Generating diff from git..."\n\
    if [ -n "$BASE_COMMIT" ]; then\n\
        git diff --unified=0 "$BASE_COMMIT"...HEAD > /tmp/gatekeeper.diff\n\
    else\n\
        git diff --unified=0 HEAD~1...HEAD > /tmp/gatekeeper.diff\n\
    fi\n\
    DIFF_FILE="/tmp/gatekeeper.diff"\n\
fi\n\
\n\
# Run AI Gatekeeper\n\
java -jar /app/ai-gatekeeper.jar \\\n\
    --diff-file "$DIFF_FILE" \\\n\
    --config "${CONFIG_FILE:-/app/.aigate.yml}" \\\n\
    --format "${OUTPUT_FORMAT:-both}" \\\n\
    --out "${OUTPUT_FILE:-/output/aigate.sarif}" \\\n\
    --verbose\n\
' > /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Create output directory
RUN mkdir -p /output

# Set entrypoint
ENTRYPOINT ["/app/entrypoint.sh"]
