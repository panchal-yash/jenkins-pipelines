library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

setup_rhel = { ->
    sh '''
    yum update -y
    sudo yum install -y https://repo.percona.com/yum/percona-release-latest.noarch.rpm
    
    echo "...checking the enabled repos.."
    percona-release show

    '''
}

setup_debian = { ->
    sh '''

    sudo apt update -y
    sudo apt install curl -y
    curl -O https://repo.percona.com/apt/percona-release_latest.generic_all.deb
    sudo apt install gnupg2 lsb-release ./percona-release_latest.generic_all.deb
    sudo apt update -y

    echo "...checking the enabled repos.."
    percona-release show

    '''
}

node_setups = [
    "min-buster-x64": setup_debian,
    "min-centos-7-x64": setup_rhel,
    "min-centos-8-x64": setup_rhel,
    "min-xenial-x64": setup_debian,
    "min-bionic-x64": setup_debian,
    "min-focal-x64": setup_debian,
]


void runNodeBuild(String node_to_test) {
    build(
        job: 'periodic-product-check-for-new-releases',
        propagate: true,
        wait: true
    )
}

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        skipDefaultCheckout()
    }

    stages {
        stage('Run parallel') {
            parallel {
                stage('Debian Stretch') {
                    steps {
                        runNodeBuild('min-stretch-x64')
                    }
                }

                stage('Debian Buster') {
                    steps {
                        runNodeBuild('min-buster-x64')
                    }
                }

                stage('Ubuntu Xenial') {
                    steps {
                        runNodeBuild('min-xenial-x64')
                    }
                }

                stage('Ubuntu Bionic') {
                    steps {
                        runNodeBuild('min-bionic-x64')
                    }
                }

                stage('Ubuntu Focal') {
                    steps {
                        runNodeBuild('min-focal-x64')
                    }
                }

                stage('Centos 7') {
                    steps {
                        runNodeBuild('min-centos-7-x64')
                    }
                }

                stage('Centos 8') {
                    steps {
                        runNodeBuild('min-centos-8-x64')
                    }
                }
            }
        }
    }
}
