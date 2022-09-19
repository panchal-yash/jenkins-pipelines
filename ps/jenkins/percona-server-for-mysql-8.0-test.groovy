/* groovylint-disable DuplicateStringLiteral, GStringExpressionWithinString, LineLength */
library changelog: false, identifier: 'lib@PS-8400-add-package-tests-ps-80-jenkins', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

void installCli(String PLATFORM) {
    sh """
        set -o xtrace
        if [ -d aws ]; then
            rm -rf aws
        fi
        if [ ${PLATFORM} = "deb" ]; then
            sudo apt-get update
            sudo apt-get -y install wget curl unzip
        elif [ ${PLATFORM} = "rpm" ]; then
            sudo yum -y install wget curl unzip
        fi
        curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip
        unzip awscliv2.zip
        sudo ./aws/install || true
    """
}

void buildStage(String DOCKER_OS, String STAGE_PARAM) {
    sh """
        set -o xtrace
        mkdir -p test
        wget \$(echo ${GIT_REPO} | sed -re 's|github.com|raw.githubusercontent.com|; s|\\.git\$||')/${BRANCH}/build-ps/percona-server-8.0_builder.sh -O ps_builder.sh || curl \$(echo ${GIT_REPO} | sed -re 's|github.com|raw.githubusercontent.com|; s|\\.git\$||')/${BRANCH}/build-ps/percona-server-8.0_builder.sh -o ps_builder.sh
        pwd -P
        ls -laR
        export build_dir=\$(pwd -P)
        set -o xtrace
        cd \${build_dir}
        if [ -f ./test/percona-server-8.0.properties ]; then
            . ./test/percona-server-8.0.properties
        fi
        sudo bash -x ./ps_builder.sh --builddir=\${build_dir}/test --install_deps=1
        if [${BUILD_TOKUDB_TOKUBACKUP} = "ON" ]; then
            bash -x ./ps_builder.sh --builddir=\${build_dir}/test --repo=${GIT_REPO} --branch=${BRANCH} --build_tokudb_tokubackup=1 --perconaft_branch=${PERCONAFT_BRANCH} --tokubackup_branch=${TOKUBACKUP_BRANCH} --rpm_release=${RPM_RELEASE} --deb_release=${DEB_RELEASE} ${STAGE_PARAM}
        else
            bash -x ./ps_builder.sh --builddir=\${build_dir}/test --repo=${GIT_REPO} --branch=${BRANCH} --perconaft_branch=${PERCONAFT_BRANCH} --tokubackup_branch=${TOKUBACKUP_BRANCH} --rpm_release=${RPM_RELEASE} --deb_release=${DEB_RELEASE} ${STAGE_PARAM}
        fi
    """
}

void cleanUpWS() {
    sh """
        sudo rm -rf ./*
    """
}


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

List all_actions = [
    "install",
]

product_action_playbooks = [
    ps80: [
        install: "ps_80.yml",
        upgrade: "ps_80_upgrade.yml",
        "maj-upgrade-to": "ps_80_major_upgrade_to.yml",
    ],
    client_test: [
        install: "client_test.yml",
        upgrade: "client_test_upgrade.yml",
    ]
]

Map product_actions = product_action_playbooks.collectEntries { key, value ->
    [key, value.keySet().collect()]
}

List actions_to_test = []
if (params.action_to_test == "all") {
    actions_to_test = all_actions
} else {
    actions_to_test = [params.action_to_test]
}

void runPlaybook(String action_to_test) {
    def playbook = product_action_playbooks[params.product_to_test][action_to_test]
    def playbook_path = "package-testing/playbooks/${playbook}"

    setup_package_tests()

    sh '''
        git clone -b PS-8400-add-package-tests-ps-80-jenkins --depth 1 https://github.com/panchal-yash/package-testing
    '''

    sh """
        export install_repo="\${install_repo}"
        export client_to_test="ps80"
        export check_warning="\${check_warnings}"
        export install_mysql_shell="\${install_mysql_shell}"
        ansible-playbook \
        --connection=local \
        --inventory 127.0.0.1, \
        --limit 127.0.0.1 \
        ${playbook_path}
    """
}

def AWS_STASH_PATH

pipeline {
    agent {
        label 'docker'
    }
parameters {
        string(defaultValue: 'https://github.com/percona/percona-server.git', description: 'github repository for build', name: 'GIT_REPO')
        string(defaultValue: 'release-8.0.28-19', description: 'Tag/Branch for percona-server repository', name: 'BRANCH')
        string(defaultValue: '1', description: 'RPM version', name: 'RPM_RELEASE')
        string(defaultValue: '1', description: 'DEB version', name: 'DEB_RELEASE')
        choice(
            choices: 'OFF\nON',
            description: 'The TokuDB storage is no longer supported since 8.0.28',
            name: 'BUILD_TOKUDB_TOKUBACKUP')
        string(defaultValue: '0', description: 'PerconaFT repository', name: 'PERCONAFT_REPO')
        string(defaultValue: 'Percona-Server-8.0.27-18', description: 'Tag/Branch for PerconaFT repository', name: 'PERCONAFT_BRANCH')
        string(defaultValue: '0', description: 'TokuBackup repository', name: 'TOKUBACKUP_REPO')
        string(defaultValue: 'Percona-Server-8.0.27-18', description: 'Tag/Branch for TokuBackup repository', name: 'TOKUBACKUP_BRANCH')
        choice(
            choices: 'ON\nOFF',
            description: 'Compile with ZenFS support?, only affects Ubuntu Hirsute',
            name: 'ENABLE_ZENFS')
        choice(
            choices: 'laboratory\ntesting\nexperimental\nrelease',
            description: 'Repo component to push packages to',
            name: 'COMPONENT')

// For Package Testing
        
        choice(
            name: "product_to_test",
            choices: ["ps80"],
            description: "Product for which the packages will be tested"
        )

        choice(
            name: "install_repo",
            choices: ["experimental"],
            description: "Repo to use in install test"
        )

        choice(
            name: "node_to_test",
            choices: all_nodes,
            description: "Node in which to test the product"
        )

        choice(
            name: "action_to_test",
            choices: ["all"] + all_actions,
            description: "Action to test on the product"
        )

        choice(
            name: "check_warnings",
            choices: ["yes", "no"],
            description: "check warning in client_test"
        )

        choice(
            name: "install_mysql_shell",
            choices: ["yes", "no"],
            description: "install and check mysql-shell for ps80"
        )            
    }
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        //timestamps()
    }
    stages {
        stage('Create PS source tarball') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                slackNotify("#new-product-release-detection-jenkins", "#00FF00", "[${JOB_NAME}]: starting build for ${BRANCH}")
                cleanUpWS()
                installCli("deb")
                buildStage("ubuntu:bionic", "--get_sources=1")
                sh '''
                   REPO_UPLOAD_PATH=$(grep "UPLOAD" test/percona-server-8.0.properties | cut -d = -f 2 | sed "s:$:${BUILD_NUMBER}:")
                   AWS_STASH_PATH=$(echo ${REPO_UPLOAD_PATH} | sed  "s:UPLOAD/experimental/::")
                   echo ${REPO_UPLOAD_PATH} > uploadPath
                   echo ${AWS_STASH_PATH} > awsUploadPath
                   cat test/percona-server-8.0.properties
                   cat uploadPath
                   cat awsUploadPath
                '''
                script {
                    AWS_STASH_PATH = sh(returnStdout: true, script: "cat awsUploadPath").trim()
                }
                stash includes: 'uploadPath', name: 'uploadPath'
                stash includes: 'test/percona-server-8.0.properties', name: 'properties'
                echo "Pushing Artifacts"
                pushArtifactFolder("source_tarball/", AWS_STASH_PATH)
                echo "UPLOAD TARBALL FROM AWS"
                uploadTarballfromAWS("source_tarball/", AWS_STASH_PATH, 'source')
            }
        }
        stage('Build PS generic source packages') {
            parallel {
                stage('Build PS generic source rpm') {
                    agent {
                        label 'min-centos-7-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_src_rpm=1")

                        pushArtifactFolder("srpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("srpm/", AWS_STASH_PATH)
                    }
                }
                stage('Build PS generic source deb') {
                    agent {
                        label 'min-bionic-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("ubuntu:bionic", "--build_source_deb=1")

                        pushArtifactFolder("source_deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("source_deb/", AWS_STASH_PATH)
                    }
                }
            }  //parallel
        } // stage
        stage('Build PS RPMs/DEBs/Binary tarballs') {
            parallel {
                stage('Centos 7') {
                    agent {
                        label 'min-centos-7-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_rpm=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Centos 8') {
                    agent {
                        label 'min-centos-8-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("centos:8", "--build_rpm=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Bionic(18.04)') {
                    agent {
                        label 'min-bionic-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("ubuntu:bionic", "--build_deb=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Focal(20.04)') {
                    agent {
                        label 'min-focal-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("ubuntu:focal", "--build_deb=1 --with_zenfs=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Debian Buster(10)') {
                    agent {
                        label 'min-buster-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("debian:buster", "--build_deb=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Debian Bullseye(11)') {
                    agent {
                        label 'min-bullseye-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("debian:bullseye", "--build_deb=1 --with_zenfs=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Centos 7 binary tarball') {
                    agent {
                        label 'min-centos-7-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_tarball=1 ")
                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Centos 7 debug tarball') {
                    agent {
                        label 'min-centos-7-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:7", "--debug=1 --build_tarball=1 ")
                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Centos 8 binary tarball') {
                    agent {
                        label 'min-centos-8-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:8", "--build_tarball=1 ")
                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Centos 8 debug tarball') {
                    agent {
                        label 'min-centos-8-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:8", "--debug=1 --build_tarball=1 ")
                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Bionic(18.04) binary tarball') {
                    agent {
                        label 'min-bionic-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("ubuntu:bionic", "--build_tarball=1 ")
                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Bionic(18.04) debug tarball') {
                    agent {
                        label 'min-bionic-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("ubuntu:bionic", "--debug=1 --build_tarball=1 ")
                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Ubuntu Focal(20.04) ZenFS tarball') {
                    agent {
                        label 'min-focal-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("ubuntu:focal", "--build_tarball=1 --with_zenfs=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
            }
        }
 
        stage('Sign packages') {
            steps {
                signRPM()
                signDEB()
            }
        }

        stage('Push to public experimental repository') {
            steps {
                // sync packages
                sync2ProdAutoBuild('ps-80', 'experimental')
            }
        }


// Start with Package testing 
        stage('Package Testing for Experimental builds parallel') {

            parallel {
                stage("Install") {
                    agent {
                        label params.node_to_test
                    }

                    when {
                        beforeAgent true
                        expression {
                            product_actions[params.product_to_test].contains("install")
                        }
                        expression {
                            actions_to_test.contains("install")
                        }
                    }

                    steps {
                        unstash 'properties'
                        runPlaybook("install")
                    }
                }

                stage("Upgrade") {
                    agent {
                        label params.node_to_test
                    }

                    when {
                        beforeAgent true
                        expression {
                            product_actions[params.product_to_test].contains("upgrade")
                        }
                        expression {
                            actions_to_test.contains("upgrade")
                        }
                    }

                    steps {
                        runPlaybook("upgrade")
                    } 
                }

                stage("Major upgrade to") {
                    agent {
                        label params.node_to_test
                    }

                    when {
                        beforeAgent true
                        expression {
                            product_actions[params.product_to_test].contains("maj-upgrade-to")
                        }
                        expression {
                            actions_to_test.contains("maj-upgrade-to")
                        }
                    }

                    steps {
                        runPlaybook("maj-upgrade-to")
                    }
                }
            }
        }

        

    }
    post {
        success {
            slackNotify("#new-product-release-detection-jenkins", "#00FF00", "[${JOB_NAME}]: build has been finished successfully for ${BRANCH}")
            deleteDir()
        }
        failure {
            slackNotify("#new-product-release-detection-jenkins", "#FF0000", "[${JOB_NAME}]: build failed for ${BRANCH}")
            deleteDir()
        }
        always {
            sh '''
                sudo rm -rf ./*
            '''
            deleteDir()
        }
    }
}
