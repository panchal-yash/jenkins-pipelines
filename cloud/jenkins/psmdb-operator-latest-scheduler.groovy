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
                stage('Trigger psmdb-operator-aks-latest job 3 times') {
                    steps {
                        build job: 'psmdb-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger psmdb-operator-gke-latest job 3 times') {
                    steps {
                        build job: 'psmdb-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger psmdb-operator-eks-latest job 3 times') {
                    steps {
                        build job: 'psmdb-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger psmdb-operator-aws-openshift-latest job 3 times') {
                    steps {
                        build job: 'psmdb-operator-aws-openshift-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-aws-openshift-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'psmdb-operator-aws-openshift-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
            }
        }
    }
    post {
        always {

            copyArtifacts(projectName: 'psmdb-operator-aks-latest', selector: lastSuccessful(), target: 'psmdb-operator-aks-latest')

            copyArtifacts(projectName: 'psmdb-operator-gke-latest', selector: lastSuccessful(), target: 'psmdb-operator-gke-latest')

            copyArtifacts(projectName: 'psmdb-operator-eks-latest', selector: lastSuccessful(), target: 'psmdb-operator-eks-latest')

            copyArtifacts(projectName: 'psmdb-operator-aws-openshift-latest', selector: lastSuccessful(), target: 'psmdb-operator-aws-openshift-latest')

            archiveArtifacts artifacts: 'psmdb-operator-aks-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'psmdb-operator-gke-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'psmdb-operator-eks-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'psmdb-operator-aws-openshift-latest/*.xml', allowEmptyArchive: true
            
            sh 'echo "----------------------------" > REPORT '

            sh 'cat psmdb-operator-aks-latest/TestsReport.xml >> REPORT'

            sh 'echo "----------------------------" >> REPORT '

            sh "cat psmdb-operator-gke-latest/TestsReport.xml >> REPORT"
            
            sh 'echo "----------------------------" >> REPORT '

            sh "cat psmdb-operator-eks-latest/TestsReport.xml >> REPORT"

            sh 'echo "----------------------------" >> REPORT '

            sh "cat psmdb-operator-aws-openshift-latest/TestsReport.xml >> REPORT"
            
            archiveArtifacts artifacts: 'REPORT'

        }
    }
}
