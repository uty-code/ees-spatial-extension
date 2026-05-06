pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './mvnw clean install'
            }
        }
        stage('Documentation Validation') {
            steps {
                // AI-Ready 문서 유효성 검사
                sh 'python -m ai_ready_scorer .'
            }
        }
    }
}
