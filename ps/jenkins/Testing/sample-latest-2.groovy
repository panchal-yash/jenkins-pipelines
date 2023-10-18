library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

pipeline {
    agent {
        label 'docker'
    }
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        timestamps ()
        copyArtifactPermission('sample-scheduler');
    }
    stages {
        stage('Create a File for Sample Test 2') {
            steps {
                sh """echo "FILE 2" > Testresults.xml"""
            }
        }
        stage('Archive artifacts') {
            steps {
                archiveArtifacts artifacts: '*.xml', allowEmptyArchive: true
            }
        }
    }
    post {
        always {
            sh '''
                sudo rm -rf ./*
            '''
            deleteDir()
        }
    }
}
