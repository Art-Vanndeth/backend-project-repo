// ==============================================================================
// Jenkins CI Pipeline - Backend Project Repository (Spring Boot 3)
// Repo: https://github.com/Art-Vanndeth/backend-project-repo.git
// Flow: Webhook -> Maven Test & SonarQube -> Docker Build -> Push Nexus ->
//       Trigger backend-config-pipeline -> Telegram Notification
// ==============================================================================

def sendTelegram(String message) {
    withCredentials([
        string(credentialsId: 'telegram-bot-token', variable: 'TG_BOT_TOKEN'),
        string(credentialsId: 'telegram-chat-id', variable: 'TG_CHAT_ID')
    ]) {
        sh """
            curl -s -X POST "https://api.telegram.org/bot\${TG_BOT_TOKEN}/sendMessage" \
              -d chat_id="\${TG_CHAT_ID}" \
              -d parse_mode="HTML" \
              --data-urlencode "text=${message}" > /dev/null
        """
    }
}

pipeline {
    agent any

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'GIT_BRANCH', value: '$.ref'],
                [key: 'GIT_PUSHER', value: '$.user_name'],
                [key: 'COMMIT_MSG', value: '$.commits[0].message']
            ],
            causeString: 'Triggered by GitLab Push Webhook',
            token: 'backend-project-token',
            printContributedVariables: true,
            printPostContent: false
        )
    }

    environment {
        NEXUS_REGISTRY = 'docker.devops.cstad.edu.kh'
        IMAGE_NAME     = 'cstad-backend'
        IMAGE_TAG      = "v1.0.${BUILD_NUMBER}"
        FULL_IMAGE     = "${NEXUS_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
    }

    stages {
        stage('Notify Pipeline Start') {
            steps {
                script {
                    sendTelegram("🚀 <b>Backend CI Started</b>\n<b>Job:</b> ${env.JOB_NAME}\n<b>Build:</b> #${env.BUILD_NUMBER}\n<b>Tag:</b> <code>${env.IMAGE_TAG}</code>")
                }
            }
        }

        stage('Checkout Private Repository') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_AUTHOR = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
                }
            }
        }

        stage('Maven Unit Test & Package') {
            steps {
                sh '''
                    docker run --rm -v "$(pwd)":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace \
                      maven:3.9.8-eclipse-temurin-17 mvn clean test
                '''
            }
        }

        stage('SonarQube Security & Code Quality Scan') {
            steps {
                script {
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                docker run --rm -v "$(pwd)":/workspace -v "$HOME/.m2":/root/.m2 -w /workspace \
                                  maven:3.9.8-eclipse-temurin-17 mvn sonar:sonar \
                                  -Dsonar.host.url="${SONAR_HOST_URL}" \
                                  -Dsonar.token="${SONAR_AUTH_TOKEN}"
                            '''
                        }
                    } catch (Exception e) {
                        echo "SonarQube server not configured yet, skipping blocking gate: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Build & Tag Docker Image') {
            steps {
                sh """
                    docker build \
                      -t ${FULL_IMAGE} \
                      -t ${NEXUS_REGISTRY}/${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Push Image to Nexus Private Registry') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-creds',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh '''
                        echo "${NEXUS_PASS}" | docker login "${NEXUS_REGISTRY}" -u "${NEXUS_USER}" --password-stdin
                        docker push "${FULL_IMAGE}"
                        docker push "${NEXUS_REGISTRY}/${IMAGE_NAME}:latest"
                        docker logout "${NEXUS_REGISTRY}"
                    '''
                }
            }
        }

        stage('Trigger Backend Config Pipeline') {
            steps {
                build job: 'backend-config-pipeline',
                    parameters: [
                        string(name: 'IMAGE_TAG', value: "${env.IMAGE_TAG}"),
                        string(name: 'ENVIRONMENT', value: 'prod')
                    ],
                    wait: false
            }
        }
    }

    post {
        success {
            script {
                sendTelegram("✅ <b>Backend CI Succeeded</b>\n<b>Image:</b> <code>${env.FULL_IMAGE}</code>\n<b>Commit:</b> <code>${env.GIT_COMMIT_SHORT}</code> (${env.GIT_AUTHOR})\n<b>Next:</b> Triggered <code>backend-config-pipeline</code>\n<b>URL:</b> ${env.BUILD_URL}")
            }
            sh 'docker rmi "${FULL_IMAGE}" || true'
        }
        failure {
            script {
                sendTelegram("❌ <b>Backend CI Failed</b>\n<b>Build:</b> #${env.BUILD_NUMBER}\n<b>Commit:</b> <code>${env.GIT_COMMIT_SHORT}</code>\n<b>URL:</b> ${env.BUILD_URL}")
            }
        }
    }
}

