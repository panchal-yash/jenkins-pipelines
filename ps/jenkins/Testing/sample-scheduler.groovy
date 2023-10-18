library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

def fetch_job_id(String JobName){

    // Get Job A
    def job = Jenkins.instance.getItem(JobName)

    // Get last successful build of Job A
    def lastSuccessfulBuild = job.getLastSuccessfulBuild()

    // Get build ID
    def buildId = lastSuccessfulBuild.getId()

    // Output or pass it to subsequent steps
    println("Last successful build ID of Job A: ${buildId}")
    return buildId
}

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
                        build job: 'sample-latest-1', propagate: false, wait: true
                        build job: 'sample-latest-1', propagate: false, wait: true
                        build job: 'sample-latest-1', propagate: false, wait: true
                    }
                }
                stage('Trigger sample-latest-2 job 3 times') {
                    steps {
                        build job: 'sample-latest-2', propagate: false, wait: true
                        build job: 'sample-latest-2', propagate: false, wait: true
                        build job: 'sample-latest-2', propagate: false, wait: true
                    }
                }
                stage('Trigger sample-latest-3 job 3 times') {
                    steps {
                        build job: 'sample-latest-3', propagate: false, wait: true
                        build job: 'sample-latest-3', propagate: false, wait: true
                        build job: 'sample-latest-3', propagate: false, wait: true
                    }
                }
            }
        }
    }
    post {
        always {
            echo "Fetch the files"
            fetch_job_id("sample-latest-1")
            fetch_job_id("sample-latest-2")
            fetch_job_id("sample-latest-3")
        }
    }
}
