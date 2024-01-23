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
                stage('Trigger sample-latest-1 job 3 times') {
                    steps {
                        script {
                            for (int i = 1; i <= 5; i++) {
                                build job: 'sample-latest-1', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                            }
                        }
                    }
                }
                stage('Trigger sample-latest-2 job 3 times') {
                    steps {
                        build job: 'sample-latest-2', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'sample-latest-2', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'sample-latest-2', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
                stage('Trigger sample-latest-3 job 3 times') {
                    steps {
                        build job: 'sample-latest-3', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'sample-latest-3', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                        build job: 'sample-latest-3', propagate: false, wait: true, parameters: [string(name: 'TEST_SUITE', value: 'run-release.csv'),string(name: 'IGNORE_PREVIOUS_RUN', value: "NO"),string(name: 'GIT_BRANCH', value: "main"),string(name: 'PLATFORM_VER', value: "latest"),string(name: 'CLUSTER_WIDE', value: "YES")]
                    }
                }
            }
        }
    }
    post {
        always {

            sh 'ls -la'
            copyArtifacts(projectName: 'sample-latest-1', selector: lastCompleted(), target: 'sample-latest-1')
            //sh '''mv TestsReport.xml sample-latest-1.xml'''
            copyArtifacts(projectName: 'sample-latest-2', selector: lastCompleted(), target: 'sample-latest-2')
            //sh '''mv TestsReport.xml sample-latest-2.xml'''
            copyArtifacts(projectName: 'sample-latest-3', selector: lastCompleted(), target: 'sample-latest-3')
            //sh '''mv TestsReport.xml sample-latest-3.xml'''
            archiveArtifacts artifacts: 'sample-latest-1/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'sample-latest-2/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'sample-latest-3/*.xml', allowEmptyArchive: true
            
            sh "sed 's/PSMDB/PSMDB-AKS/' sample-latest-1/*"

            sh 'echo "----------------------------" > REPORT '

            sh "sed 's/PSMDB/PSMDB-AKS/' sample-latest-1/TestsReport.xml >> REPORT"

            sh 'echo "----------------------------" >> REPORT '

            sh "sed 's/PSMDB/PSMDB-GKE/' sample-latest-2/TestsReport.xml >> REPORT"
            
            sh 'echo "----------------------------" >> REPORT '

            sh "sed 's/PSMDB/PSMDB-EKS/' sample-latest-3/TestsReport.xml >> REPORT"
            
            archiveArtifacts artifacts: 'REPORT'

//            # Show test result with JUnit test results.
        }
    }
}
