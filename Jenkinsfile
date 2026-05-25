pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'localhost:5000'
        VAULT_ADDR      = 'http://vault:8200'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Common Library') {
            steps {
                dir('episante-common') {
                    sh './mvnw clean install -DskipTests'
                }
            }
        }

        stage('Build & Test Backend Services') {
            parallel {
                stage('auth-service') {
                    steps {
                        dir('auth-service') {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
                stage('doctor-service') {
                    steps {
                        dir('doctor-service') {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
                stage('patient-service') {
                    steps {
                        dir('patient-service') {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
                stage('appointment-service') {
                    steps {
                        dir('appointment-service') {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
                stage('ingest-wearables-service') {
                    steps {
                        dir('ingest-wearables-service') {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
                stage('health-alert-engine') {
                    steps {
                        dir('health-alert-engine') {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npx tsc --noEmit'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker Build & Push') {
            parallel {
                stage('auth-service image') {
                    steps {
                        sh "docker build -f auth-service/Dockerfile -t ${DOCKER_REGISTRY}/auth-service:${BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_REGISTRY}/auth-service:${BUILD_NUMBER}"
                    }
                }
                stage('doctor-service image') {
                    steps {
                        sh "docker build -f doctor-service/Dockerfile -t ${DOCKER_REGISTRY}/doctor-service:${BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_REGISTRY}/doctor-service:${BUILD_NUMBER}"
                    }
                }
                stage('patient-service image') {
                    steps {
                        sh "docker build -f patient-service/Dockerfile -t ${DOCKER_REGISTRY}/patient-service:${BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_REGISTRY}/patient-service:${BUILD_NUMBER}"
                    }
                }
                stage('appointment-service image') {
                    steps {
                        sh "docker build -f appointment-service/Dockerfile -t ${DOCKER_REGISTRY}/appointment-service:${BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_REGISTRY}/appointment-service:${BUILD_NUMBER}"
                    }
                }
                stage('ingest-wearables image') {
                    steps {
                        sh "docker build -f ingest-wearables-service/Dockerfile -t ${DOCKER_REGISTRY}/ingest-wearables-service:${BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_REGISTRY}/ingest-wearables-service:${BUILD_NUMBER}"
                    }
                }
                stage('health-alert-engine image') {
                    steps {
                        sh "docker build -f health-alert-engine/Dockerfile -t ${DOCKER_REGISTRY}/health-alert-engine:${BUILD_NUMBER} ."
                        sh "docker push ${DOCKER_REGISTRY}/health-alert-engine:${BUILD_NUMBER}"
                    }
                }
                stage('frontend image') {
                    steps {
                        sh "docker build -f frontend/Dockerfile -t ${DOCKER_REGISTRY}/frontend:${BUILD_NUMBER} frontend/"
                        sh "docker push ${DOCKER_REGISTRY}/frontend:${BUILD_NUMBER}"
                    }
                }
                stage('notification-service image') {
                    steps {
                        sh "docker build -f notification-service/Dockerfile -t ${DOCKER_REGISTRY}/notification-service:${BUILD_NUMBER} notification-service/"
                        sh "docker push ${DOCKER_REGISTRY}/notification-service:${BUILD_NUMBER}"
                    }
                }
            }
        }

        stage('Deploy to VMs') {
            when {
                branch 'master'
            }
            steps {
                sh '''
                    cd infra/compose
                    docker-compose pull
                    docker-compose up -d --remove-orphans
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed — check logs above.'
        }
    }
}
