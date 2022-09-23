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

setup_rhel_8_package_tests = { ->
    sh '''

        sudo yum install python3 python3-pip -y
        sudo pip3 install ansible


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
    "min-ol-8-x64": setup_rhel_8_package_tests,
    "min-centos-7-x64": setup_rhel_package_tests,
    "min-bionic-x64": setup_ubuntu_package_tests,
    "min-focal-x64": setup_ubuntu_package_tests,
    "min-amazon-2-x64": setup_amazon_package_tests,
    "min-centos-8-x64": setup_rhel_8_package_tests,
]

void setup_package_tests() {
    node_setups[params.node_to_test]()
}

List all_nodes = node_setups.keySet().collect()


List actions_to_test = []
if (params.action_to_test == "all") {
    actions_to_test = all_actions
} else {
    actions_to_test = [params.action_to_test]
}

void runPlaybook(String percona_server_repository, String percona_server_version) {
    
    setup_package_tests()

    sh """
        wget https://raw.githubusercontent.com/panchal-yash/percona-server/main/playbook.yml
        ansible-galaxy install panchal_yash.percona_server
        ansible-galaxy install -p /mnt/jenkins/workspace/ansible-galaxy-roles-build/roles/ panchal_yash.percona_server
        sed -i -e 's/VERSION/${percona_server_version}/g' -e 's/REPOSITORY/${percona_server_repository}/g' playbook.yml
        sudo ansible-playbook \
        --connection=local \
        --inventory 127.0.0.1, \
        --limit 127.0.0.1 \
        playbook.yml

        echo "Checking for the Mysql Version"

        mysql --version
    """
}

pipeline {
    agent none

    options {
        skipDefaultCheckout()
    }

    parameters{

        choice(
            name: "node_to_test",
            choices: all_nodes,
            description: "Node in which to test the product"
        )

    }


    stages {
        stage("Prepare") {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}-${params.node_to_test}"
                }
            }
        }
        stage("Run parallel") {
            parallel {
// main
                stage("main and 8.0") {

                    
                    agent {
                        label params.node_to_test
                    }
                    steps {
                        runPlaybook("main" , "8.0")
                    }
                }
                
                stage("main and 5.7") {
                    agent {
                        label params.node_to_test
                    }
                    steps {
                        runPlaybook("main" , "5.7")
                    }
                }
// testing
                stage("testing and 8.0") {
                    agent {
                        label params.node_to_test
                    }
                    steps {
                        runPlaybook("testing" , "8.0")
                    }
                }
                
                stage("testing and 5.7") {
                    agent {
                        label params.node_to_test
                    }
                    steps {
                        runPlaybook("testing" , "5.7")
                    }
                }

// experimental
                stage("experimental and 8.0") {
                    agent {
                        label params.node_to_test
                    }
                    steps {
                        runPlaybook("experimental" , "8.0")
                    }
                }
                
                stage("experimental and 5.7") {
                    agent {
                        label params.node_to_test
                    }
                    steps {
                        runPlaybook("experimental" , "5.7")
                    }
                }

            }
        }
    }
}
