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
             script{


                    withCredentials([string(credentialsId: 'PXC_GITHUB_API_TOKEN', variable: 'TOKEN')]) {

                    sh """
 
                    git clone https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git

                    cd package-testing

                    cat .git/config

                    git checkout test

                    cat .git/config

                    git status

                    git config user.name "jenkins-pxc-cd"
                    git config user.email "it+jenkins-pxc-cd@percona.com"

                    cat .git/config

                    """

                    }
             }

            }
        }
        
    }




    }
