library changelog: false, identifier: "lib@master", retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
])

pipeline {
    agent {
        label "docker"
    }    
    environment {
        PATH = '/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/home/ec2-user/.local/bin'
    }
    options {
        disableConcurrentBuilds()
    }
    triggers { 
        cron('0 0 * * 0') 
    }
    parameters {
        string(
            name: 'PXB_VERSION',
            defaultValue: 'latest',
            description: 'Enter a VERSION of docker image to test'
        )
        string(
            name: 'DOCKER_REPO',
            defaultValue: 'percona',
            description: 'Enter percona (Released Images) or perconalab (Testing Images)'
        )
    }
    stages {
        stage ('Run tests') {
            steps {
                sh """
                    TRIVY_VERSION=\$(curl --silent 'https://api.github.com/repos/aquasecurity/trivy/releases/latest' | grep '"tag_name":' | tr -d '"' | sed -E 's/.*v(.+),.*/\\1/')
                    wget https://github.com/aquasecurity/trivy/releases/download/v\${TRIVY_VERSION}/trivy_\${TRIVY_VERSION}_Linux-64bit.tar.gz
                    sudo tar zxvf trivy_\${TRIVY_VERSION}_Linux-64bit.tar.gz -C /usr/local/bin/
                    wget https://raw.githubusercontent.com/aquasecurity/trivy/v\${TRIVY_VERSION}/contrib/junit.tpl
                    curl https://raw.githubusercontent.com/Percona-QA/psmdb-testing/main/docker/trivyignore -o ".trivyignore"
                    /usr/local/bin/trivy -q image --format template --template @junit.tpl  -o trivy-hight-junit.xml \
                                --timeout 10m0s --ignore-unfixed --exit-code 0 --severity HIGH,CRITICAL "${params.DOCKER_REPO}/percona-xtrabackup:${params.PXB_VERSION}"
                """
            }
            post {
                always {
                    junit testResults: "*-junit.xml", keepLongStdio: true, allowEmptyResults: true, skipPublishingChecks: true
                    sh """
                        docker kill \$(docker ps -a -q) || true
                        docker rm \$(docker ps -a -q) || true
                        docker rmi -f \$(docker images -q | uniq) || true
                        sudo rm -rf ./*
                    """
                }
            }
        }
    }
    post {
        always {
            sh """
                sudo docker rmi -f \$(sudo docker images -q | uniq) || true
                sudo rm -rf ${WORKSPACE}/*
            """
            deleteDir()
        }
        success {
            slackNotify("#dev-server-qa", "#00FF00", "[${JOB_NAME}]: Testing PXC docker images for CVE - succeed")
        }
        unstable {
            slackNotify("#dev-server-qa", "#F6F930", "[${JOB_NAME}]: Testing PXC docker images for CVE - some issues found: [${BUILD_URL}testReport/]")
        }
        failure {
            slackNotify("#dev-server-qa", "#FF0000", "[${JOB_NAME}]: Testing PXC docker images for CVE - unexpected failure: [${BUILD_URL}]")
        }
    }
}
