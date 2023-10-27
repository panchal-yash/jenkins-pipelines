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
                stage('Trigger pxc-operator-aks-latest job 3 times') {
                    steps {
                        build job: 'pxc-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-aks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger pxc-operator-gke-latest job 3 times') {
                    steps {
                        build job: 'pxc-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-gke-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger pxc-operator-eks-latest job 3 times') {
                    steps {
                        build job: 'pxc-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-eks-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger pxc-operator-aws-openshift-latest job 3 times') {
                    steps {
                        build job: 'pxc-operator-aws-openshift-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-aws-openshift-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'pxc-operator-aws-openshift-latest', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
            }
        }
    }
    post {
        always {

            copyArtifacts(projectName: 'pxc-operator-aks-latest', selector: lastSuccessful(), target: 'pxc-operator-aks-latest')

            copyArtifacts(projectName: 'pxc-operator-gke-latest', selector: lastSuccessful(), target: 'pxc-operator-gke-latest')

            copyArtifacts(projectName: 'pxc-operator-eks-latest', selector: lastSuccessful(), target: 'pxc-operator-eks-latest')

            copyArtifacts(projectName: 'pxc-operator-aws-openshift-latest', selector: lastSuccessful(), target: 'pxc-operator-aws-openshift-latest')

            archiveArtifacts artifacts: 'pxc-operator-aks-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'pxc-operator-gke-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'pxc-operator-eks-latest/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'pxc-operator-aws-openshift-latest/*.xml', allowEmptyArchive: true
            
            sh 'echo "----------------------------" > REPORT '

            sh 'cat pxc-operator-aks-latest/TestsReport.xml >> REPORT'

            sh 'echo "----------------------------" >> REPORT '

            sh "cat pxc-operator-gke-latest/TestsReport.xml >> REPORT"
            
            sh 'echo "----------------------------" >> REPORT '

            sh "cat pxc-operator-eks-latest/TestsReport.xml >> REPORT"

            sh 'echo "----------------------------" >> REPORT '

            sh "cat pxc-operator-aws-openshift-latest/TestsReport.xml >> REPORT"
            
            archiveArtifacts artifacts: 'REPORT'

        }
    }
}
