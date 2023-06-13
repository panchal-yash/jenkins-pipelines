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
    
                echo "Check"

                    echo "${JENKINS_HOME}"

                    sh """
                    set +xe 
                    sudo yum install jq -y
                    cd ${JENKINS_HOME}
                    wget https://raw.githubusercontent.com/panchal-yash/package-testing/ec-instance-checks/scripts/check-ec2-instances.sh
                    chmod +x check-ec2-instances.sh
                    """
             script{


                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'c42456e5-c28d-4962-b32c-b75d161bff27', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {

                        env.DAYS = "3"



                        //env.CHECK = sh(returnStdout: true).trim()

sh """
set +xe
#!/bin/bash
awsRegions=("us-west-1" "us-west-2")
#
days="3"
for region in "\$awsRegions[@]"
do


echo "----------------"$region"----------------------"
        ./check-ec2-instances.sh "$region" "$days"
echo "----------------------------------------------"
done

                        """


                    }

                        echo "Print the output"
                        echo "${env.CHECK}"
                        echo "${env.DAYS}"
             }

            }
        }
        
    }


    post {

        always {
             catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                script{
                    slackSend channel: '#dev-server-qa', color: '#DEFF13', message: """
                    
${env.CHECK}
                    
                    """
                
                
                }
             }
        }

    }

}
