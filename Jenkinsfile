pipeline {
    agent { 
        label 'linux && java21'  // Adjust label to match your Jenkins agents
    }
    
    environment {
        ANTHROPIC_API_KEY = credentials('ANTHROPIC_API_KEY')  // Store API key in Jenkins credentials
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git fetch --all --prune --tags'
            }
        }
        
        stage('Build Gatekeeper') {
            steps {
                sh 'mvn -q -DskipTests clean package'
                archiveArtifacts artifacts: 'target/ai-gatekeeper-*.jar', fingerprint: true
            }
        }
        
        stage('Compute Diff') {
            steps {
                sh '''
                    set -e
                    echo "Computing diff for review..."
                    
                    # Determine base commit based on build type
                    if [ -n "$CHANGE_ID" ] && [ -n "$CHANGE_TARGET" ]; then
                        echo "PR build detected: PR #$CHANGE_ID targeting $CHANGE_TARGET"
                        BASE=$(git merge-base HEAD origin/"$CHANGE_TARGET")
                        echo "Base commit: $BASE"
                    else
                        echo "Branch build detected"
                        git fetch origin main
                        BASE=$(git merge-base HEAD origin/main)
                        echo "Base commit: $BASE"
                    fi
                    
                    # Generate unified diff
                    git diff --unified=0 "$BASE"...HEAD > gatekeeper.diff
                    
                    # Show diff statistics
                    echo "Diff statistics:"
                    echo "  Files changed: $(grep -c '^diff --git' gatekeeper.diff || echo 0)"
                    echo "  Lines added: $(grep -c '^+' gatekeeper.diff || echo 0)"
                    echo "  Lines removed: $(grep -c '^-' gatekeeper.diff || echo 0)"
                    echo "  Diff size: $(wc -c < gatekeeper.diff) bytes"
                    
                    # Archive diff for debugging
                    archiveArtifacts artifacts: 'gatekeeper.diff', fingerprint: true
                '''
            }
        }
        
        stage('AI Review') {
            steps {
                sh '''
                    set -e
                    echo "Starting AI code review..."
                    
                    # Create output directory
                    mkdir -p build
                    
                    # Run the gatekeeper
                    java -jar target/ai-gatekeeper-*.jar \
                        --diff-file gatekeeper.diff \
                        --config .aigate.yml \
                        --format both \
                        --out build/aigate.sarif \
                        --verbose
                    
                    echo "AI review completed"
                '''
            }
        }
        
        stage('Publish Reports') {
            when { 
                expression { fileExists('build/aigate.sarif') } 
            }
            steps {
                archiveArtifacts artifacts: 'build/aigate.sarif', fingerprint: true
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'build',
                    reportFiles: 'aigate.sarif',
                    reportName: 'AI Gatekeeper SARIF Report'
                ])
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    // The gatekeeper CLI will exit with appropriate codes:
                    // 0 = PASS, 2 = WARN, 1 = FAIL
                    // Jenkins will automatically fail the build on exit code 1
                    echo "Quality gate evaluation completed by CLI exit code"
                }
            }
        }
    }
    
    post {
        always {
            // Clean up workspace
            cleanWs()
        }
        
        success {
            echo 'AI Gatekeeper review passed successfully'
        }
        
        failure {
            echo 'AI Gatekeeper review failed - check the console output and SARIF report for details'
        }
        
        unstable {
            echo 'AI Gatekeeper review completed with warnings'
        }
    }
}
