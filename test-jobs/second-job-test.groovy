/* groovylint-disable DuplicateStringLiteral, GStringExpressionWithinString, LineLength */
library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

void cleanUpWS() {
    sh """
        sudo rm -rf ./*
    """
}

pipeline {
    agent {
        label 'docker'
    }

    parameters {
        string(defaultValue: 'REVISION1', description: 'github repository for build', name: 'option1')
        string(defaultValue: 'BRANCHa', description: 'Tag/Branch for percona-server repository', name: 'option2')
    }
    
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        timestamps ()
    }
    stages {
        stage('First Stage in 2nd ') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                cleanUpWS()

                script {
                    echo "First Stage in 2nd and ${option2}"
                }

            }
        }
        stage('Second Stage in 2nd') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                cleanUpWS()

                script {
                    echo "Second Stage in 2nd"
                }

            }
        }
    
    }

    post {
        success {
            script {
                    echo "Success"
            }
            deleteDir()
        }
        failure {
            script {
                    echo "Failed"
            }
            deleteDir()
        }
        always {
            sh '''
                sudo rm -rf ./*
            '''
            deleteDir()
        }
    }
}
