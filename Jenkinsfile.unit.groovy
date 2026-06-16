pipeline {
    agent {
        label 'docker'
    }
    
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }
    
    stages {
        stage('Source') {
            steps {
                echo 'Cloning repository...'
                git 'https://github.com/srayuso/unir-cicd.git'
            }
        }
        
        stage('Build') {
            steps {
                echo 'Building stage!'
                sh 'make build'
            }
        }
        
        stage('Unit tests') {
            steps {
                echo 'Running Unit tests...'
                sh 'make test-unit'
            }
        }
        
        stage('API tests') {
            steps {
                echo 'Running API tests...'
                sh 'make test-api'
            }
        }
        
        stage('E2E tests') {
            steps {
                echo 'Running E2E tests...'
                sh 'make test-e2e'
            }
        }
    }
    
    post {
        always {
            echo 'Archiving test results...'
            archiveArtifacts artifacts: 'results/*.xml', allowEmptyArchive: true
            
            echo 'Publishing test reports...'
            junit testResults: 'results/*_result.xml', allowEmptyResults: true
        }
        
        failure {
            echo 'Pipeline failed! Sending failure notification...'
            script {
                def jobName = env.JOB_NAME
                def buildNumber = env.BUILD_NUMBER
                def buildUrl = env.BUILD_URL
                
                echo """
                =====================================
                FALLO EN LA EJECUCIÓN DEL PIPELINE
                =====================================
                Nombre del trabajo: ${jobName}
                Número de ejecución: ${buildNumber}
                URL de la ejecución: ${buildUrl}
                =====================================
                """
                
                // Descomentar la siguiente línea para enviar correo
                // emailext (
                //     recipientProviders: [developers(), requestor()],
                //     subject: "Fallo en el pipeline: ${jobName} #${buildNumber}",
                //     body: """
                //     El pipeline ha fallado.
                //     
                //     Nombre del trabajo: ${jobName}
                //     Número de ejecución: ${buildNumber}
                //     URL: ${buildUrl}
                //     
                //     Por favor, revise los logs de la ejecución.
                //     """
                // )
            }
        }
        
        success {
            echo 'Pipeline executed successfully!'
        }
        
        cleanup {
            cleanWs()
        }
    }
}