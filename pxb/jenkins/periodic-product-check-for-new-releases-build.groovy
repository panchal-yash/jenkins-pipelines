library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _


setup_debian = { ->
    sh '''
        sudo apt-get update -y
        sudo apt install curl -y
        curl -O https://repo.percona.com/apt/percona-release_latest.generic_all.deb
        sudo apt install gnupg2 lsb-release ./percona-release_latest.generic_all.deb
        sudo apt update -y

        percona-release show
    '''
}

setup_rhel = { ->
    sh '''
        sudo yum update -y
        sudo yum install -y https://repo.percona.com/yum/percona-release-latest.noarch.rpm

        percona-release show
    '''
}


node_setups = [
    "min-bullseye-x64": setup_debian,
    "min-buster-x64": setup_debian,
    "min-centos-7-x64": setup_rhel,
    "min-ol-8-x64": setup_rhel,
    "min-bionic-x64": setup_debian,
    "min-focal-x64": setup_debian,
    "min-amazon-2-x64": setup_rhel,
]

void setup_package_tests() {
    node_setups[params.node_to_test]()
}

List all_nodes = node_setups.keySet().collect()


pipeline {
    label params.node_to_test

    options {
        skipDefaultCheckout()
    }
    
    parameters{
        choice(
            name: "node_to_test",
            choices: all_nodes,
            description: "Node in which to test the script"
        )
    }


    stages {
        stage("Prepare") {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}"
                    currentBuild.description = "action: install and check the percona-release"
                }
            }
        }

        stage("Setup the Server"){
            steps {
                setup_package_tests()
            }
        }

        stage("check os") {
            steps {
                echo "cat /etc/os-release"
            }
        }

        stage("check if percona-release is installed") {
            steps {
                sh "percona-release"
            }
        }

    }
}
