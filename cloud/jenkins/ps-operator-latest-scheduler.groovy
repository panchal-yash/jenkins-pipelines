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
    }
    stages {
        stage("Run parallel") {
            parallel{
                stage('Trigger ps-operator-aks-latest job 3 times') {
                    steps {
                        build job: 'ps-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'ps-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'ps-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger ps-operator-gke-latest job 3 times') {
                    steps {
                        build job: 'ps-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'ps-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'ps-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger ps-operator-eks-latest job 3 times') {
                    steps {
                        build job: 'ps-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'ps-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'ps-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
            }
        }
    }
    post {
        always {

            copyArtifacts(projectName: 'ps-operator-aks-latest', selector: lastSuccessful(), target: 'ps-operator-aks-latest')

            copyArtifacts(projectName: 'ps-operator-gke-latest', selector: lastSuccessful(), target: 'ps-operator-gke-latest')

            copyArtifacts(projectName: 'ps-operator-eks-latest', selector: lastSuccessful(), target: 'ps-operator-eks-latest')

            archiveArtifacts artifacts: 'ps-operator-aks-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'ps-operator-gke-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'ps-operator-eks-latest/*.xml', allowEmptyArchive: true
            
            sh "cat ps-operator-aks-latest/*"

            sh 'echo "----------------------------" > REPORT '

            sh 'cat ps-operator-aks-latest/TestsReport.xml >> REPORT'

            sh 'echo "----------------------------" >> REPORT '

            sh "cat ps-operator-gke-latest/TestsReport.xml >> REPORT"
            
            sh 'echo "----------------------------" >> REPORT '

            sh "cat ps-operator-eks-latest/TestsReport.xml >> REPORT"
            
            archiveArtifacts artifacts: 'REPORT'

        }
    }
}
