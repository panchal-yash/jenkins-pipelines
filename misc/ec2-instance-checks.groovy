library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

pipeline {
    agent {
        label 'micro-amazon'
    }

    options {
        skipDefaultCheckout()
    }

    stages {


        stage("Cleanup Workspace") {
            steps {                
                sh "sudo rm -rf ${WORKSPACE}/*"
            }
        }

        stage("Checks") {

            

            steps {              
                    sh """
                    set +xe 
                    sudo yum install jq -y
                    wget https://raw.githubusercontent.com/panchal-yash/package-testing/ec-instance-checks/scripts/check-ec2-instances.sh
                    sudo wget -qO /opt/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
                    sudo chmod +x /opt/yq
                    chmod +x check-ec2-instances.sh
                    """
             script{


                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'c42456e5-c28d-4962-b32c-b75d161bff27', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {

                        env.CHECK = sh(script: "./check-ec2-instances.sh",returnStdout: true).trim()

                    }

                        echo "Print the output"
                        echo "${env.CHECK}"

             }

            }
        }
        
    }


    post {

        always {
             catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                script{
                    slackSend channel: '#dev-server-qa', color: '#DEFF13', message: """${env.CHECK}"""
                }
             }
        }

    }

}
