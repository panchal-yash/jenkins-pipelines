library changelog: false, identifier: 'lib@ansible-galaxy-role', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/panchal-yash/jenkins-pipelines.git'
]) _

setup_rhel_package_tests = { ->
    sh '''
        sudo yum install -y epel-release
        sudo yum -y update
        sudo yum install -y ansible git wget tar
    '''
}

setup_amazon_package_tests = { ->
    sh '''
        sudo amazon-linux-extras install epel
        sudo yum -y update
        sudo yum install -y ansible git wget
    '''
}

setup_stretch_package_tests = { ->
    sh '''
        sudo apt-get update
        sudo apt-get install -y dirmngr gnupg2
        echo "deb http://ppa.launchpad.net/ansible/ansible/ubuntu trusty main" | sudo tee -a /etc/apt/sources.list > /dev/null
        sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 93C4A3FD7BB9C367
        sudo apt-get update
        sudo apt-get install -y ansible git wget
    '''
}

setup_debian_package_tests = { ->
    sh '''
        sudo apt-get update
        sudo apt-get install -y ansible git wget
    '''
}

setup_ubuntu_package_tests = { ->
    sh '''
        sudo apt-get update
        sudo apt-get install -y software-properties-common
        sudo apt-add-repository --yes --update ppa:ansible/ansible
        sudo apt-get install -y ansible git wget
    '''
}

node_setups = [
    "min-buster-x64": setup_debian_package_tests,
    "min-bullseye-x64": setup_debian_package_tests,
    "min-ol-8-x64": setup_rhel_package_tests,
    "min-centos-7-x64": setup_rhel_package_tests,
    "min-bionic-x64": setup_ubuntu_package_tests,
    "min-focal-x64": setup_ubuntu_package_tests,
    "min-amazon-2-x64": setup_amazon_package_tests,
]

void setup_package_tests() {
    node_setups[params.node_to_test]()
}

List all_nodes = node_setups.keySet().collect()

def percona_server_version = params.percona_server_version

def percona_server_repository = params.percona_server_repository

void runPlaybook(String percona_server_repository, String percona_server_version) {
    
    setup_package_tests()

    sh """
        wget https://raw.githubusercontent.com/panchal-yash/percona-server/main/playbook.yml
        ansible-galaxy install panchal_yash.percona_server
        sed -i -e 's/VERSION/${percona_server_version}/g' -e 's/REPOSITORY/${percona_server_repository}/g' playbook.yml
        sudo ansible-playbook \
        --connection=local \
        --inventory 127.0.0.1, \
        --limit 127.0.0.1 \
        playbook.yml
    """
}

pipeline {
    agent none

    options {
        skipDefaultCheckout()
    }

    parameters{

        choice(
            name: "percona_server_version",
            choices: ["8.0" , "5.7"]
            description: "Percona Server Version"
        )

        choice(
            name: "percona_server_repository",
            choices: ["main" , "testing" , "experimental"]
            description: "Percona Server Repository"
        )

    }


    stages {
        stage("Prepare") {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}-${percona_server_version}-${percona_server_repository}"
                }
            }
        }

        stage("Run parallel") {

            parallel {

                stage("Buster") {

                    agent {
                        label "min-buster-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }
                
                stage("Bullseye") {
                    agent {
                        label "min-bullseye-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }

                stage("OL-8") {
                    agent {
                        label "min-ol-8-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }

                stage("Centos 7") {
                    agent {
                        label "min-centos-7-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }

                stage("Bionic") {
                    agent {
                        label "min-bionic-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }
                
                stage("Focal") {
                    agent {
                        label "min-focal-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }

                stage("Amazon Linux") {
                    agent {
                        label "min-amazon-2-x64"
                    }
                    steps {
                        runPlaybook(percona_server_repository , percona_server_version)
                    }
                }

            }
        }
    }
}
