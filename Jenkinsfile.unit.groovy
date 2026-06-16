pipeline {
    agent any
    
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }
    
    stages {
        stage('Cleanup') {
            steps {
                echo 'Cleaning up old containers and networks...'
                sh '''
                    export PATH="/usr/local/bin:$PATH"
                    # Kill all containers that might be using ports
                    /usr/local/bin/docker kill $(docker ps -a -q) 2>/dev/null || true
                    sleep 2
                    # Remove all stopped containers
                    /usr/local/bin/docker rm -f $(docker ps -a -q) 2>/dev/null || true
                    # Remove old networks
                    /usr/local/bin/docker network rm calc-test-api calc-test-e2e 2>/dev/null || true
                    echo "Cleanup completed"
                '''
            }
        }
        
        stage('Source') {
            steps {
                echo 'Cloning repository...'
                git 'https://github.com/srayuso/unir-cicd.git'
            }
        }
        
        stage('Build') {
            steps {
                echo 'Building stage!'
                sh 'export PATH="/usr/local/bin:$PATH" && make build'
            }
        }
        
        stage('Unit tests') {
            steps {
                echo 'Running Unit tests...'
                sh 'export PATH="/usr/local/bin:$PATH" && make test-unit'
            }
        }
        
        stage('API tests') {
            steps {
                echo 'Running API tests...'
                sh 'export PATH="/usr/local/bin:$PATH" && docker network create calc-test-api || true'
                sh 'export PATH="/usr/local/bin:$PATH" && docker run -d --network calc-test-api --env PYTHONPATH=/opt/calc --name apiserver --env FLASK_APP=app/api.py -p 5001:5000 -w /opt/calc calculator-app:latest flask run --host=0.0.0.0'
                sh 'sleep 3 && export PATH="/usr/local/bin:$PATH" && docker run --network calc-test-api --name api-tests --env PYTHONPATH=/opt/calc --env BASE_URL=http://apiserver:5000/ -w /opt/calc calculator-app:latest pytest --junit-xml=results/api_result.xml -m api || true'
                sh 'export PATH="/usr/local/bin:$PATH" && docker cp api-tests:/opt/calc/results ./ || true'
                sh 'export PATH="/usr/local/bin:$PATH" && docker stop apiserver api-tests 2>/dev/null || true && docker rm --force apiserver api-tests 2>/dev/null || true && docker network rm calc-test-api 2>/dev/null || true'
            }
        }
        
        stage('E2E tests') {
            steps {
                echo 'Running E2E tests...'
                sh 'export PATH="/usr/local/bin:$PATH" && make test-e2e'
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