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

def AWS_STASH_PATH

pipeline {
    agent {
        label 'docker'
    }
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        timestamps ()
    }
    stages {
        stage('First Stage') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                cleanUpWS()

                script {
                    echo "First Stage"
                    echo "-----------writing the properties file---------"
                }
                sh '''
                
                echo "REVISION=12312312" > test.properties
                echo "NAME=YASH" >> test.properties
                
                '''
                stash includes: 'test.properties', name: 'properties'

            }
        }
        stage('Second Stage') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                cleanUpWS()

                script {
                    echo "Second Stage"
                }

            }
        }
    
        stage('Run Package Tests Stage') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                cleanUpWS()
                unstash 'properties'                
                script {
                    REVISION = sh(returnStdout: true, script: "grep REVISION test.properties | awk -F '=' '{ print\$2 }'").trim()
                    echo "${REVISION}"
                    build job: 'second-job-test', parameters: [string(name: 'option1', value: 'min-bullseye-x64'),string(name: 'option2', value: "${REVISION}")]
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
                currentBuild.description = "Built on ${REVISION}"
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
