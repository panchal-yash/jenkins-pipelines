library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _
    def awsCredentials = [
        sshUserPrivateKey(
            credentialsId: 'MOLECULE_AWS_PRIVATE_KEY',
            keyFileVariable: 'MOLECULE_AWS_PRIVATE_KEY',
            passphraseVariable: '',
            usernameVariable: ''
        ),
        aws(
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            credentialsId: '5d78d9c7-2188-4b16-8e31-4d5782c6ceaa',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        )
    ]

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
    
                echo "Check"

                withCredentials(awsCredentials) {
                    sh """
                        echo JELO
                        
                        wget https://raw.githubusercontent.com/panchal-yash/package-testing/ec-instance-checks/scripts/check-ec2-instances.sh

                        chmod +x check-ec2-instances.sh

                        ./check-ec2-instances.sh us-east-1 3 | jq -r '.[] | select(length > 0)'

                    """
                }

            }
        }
        
    }


    post {

        always {
             catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                archiveArtifacts artifacts: 'PXC/**/*.tar.gz' , followSymlinks: false
             }
        }

        unstable {
                slackSend channel: '#dev-server-qa', color: '#DEFF13', message: "[${env.JOB_NAME}]: Failed during the Package testing (Unstable Build) [${env.BUILD_URL}] Parameters: product_to_test: ${params.product_to_test} , node_to_test: ${params.node_to_test} , test_repo: ${params.test_repo}, test_type: ${params.test_type}"
        }

        failure {
                slackSend channel: '#dev-server-qa', color: '#FF0000', message: "[${env.JOB_NAME}]: Failed during the Package testing (Build Failed) [${env.BUILD_URL}] Parameters: product_to_test: ${params.product_to_test} , node_to_test: ${params.node_to_test} , test_repo: ${params.test_repo}, test_type: ${params.test_type}"
        }


    }

}
