/* groovylint-disable DuplicateStringLiteral, GStringExpressionWithinString, LineLength */
library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

import groovy.transform.Field

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
        export build_dir=\$(pwd -P)
        if [ "$DOCKER_OS" = "none" ]; then
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
        else
            docker run -u root -v \${build_dir}:\${build_dir} ${DOCKER_OS} sh -c "
                set -o xtrace
                cd \${build_dir}
                if [ -f ./test/percona-server-8.0.properties ]; then
                    . ./test/percona-server-8.0.properties
                fi
                bash -x ./ps_builder.sh --builddir=\${build_dir}/test --install_deps=1
                if [ ${BUILD_TOKUDB_TOKUBACKUP} = \"ON\" ]; then
                    bash -x ./ps_builder.sh --builddir=\${build_dir}/test --repo=${GIT_REPO} --branch=${BRANCH} --build_tokudb_tokubackup=1 --perconaft_branch=${PERCONAFT_BRANCH} --tokubackup_branch=${TOKUBACKUP_BRANCH} --rpm_release=${RPM_RELEASE} --deb_release=${DEB_RELEASE} ${STAGE_PARAM}
                else
                    bash -x ./ps_builder.sh --builddir=\${build_dir}/test --repo=${GIT_REPO} --branch=${BRANCH} --perconaft_branch=${PERCONAFT_BRANCH} --tokubackup_branch=${TOKUBACKUP_BRANCH} --rpm_release=${RPM_RELEASE} --deb_release=${DEB_RELEASE} ${STAGE_PARAM}
                fi"
        fi
    """
}

void cleanUpWS() {
    sh """
        sudo rm -rf ./*
    """
}

def installDependencies(def nodeName) {
    def aptNodes = ['min-bullseye-x64', 'min-bookworm-x64', 'min-bionic-x64', 'min-focal-x64', 'min-jammy-x64']
    def yumNodes = ['min-ol-8-x64', 'min-centos-7-x64', 'min-ol-9-x64']
    try{
        if (aptNodes.contains(nodeName)) {
            if(nodeName == "min-bullseye-x64" || nodeName == "min-bookworm-x64"){            
                sh '''
                    sudo apt-get update
                    sudo apt-get install -y ansible git wget
                '''
            }else if(nodeName == "min-bionic-x64" || nodeName == "min-focal-x64" || nodeName == "min-jammy-x64"){
                sh '''
                    sudo apt-get update
                    sudo apt-get install -y software-properties-common
                    sudo apt-add-repository --yes --update ppa:ansible/ansible
                    sudo apt-get install -y ansible git wget
                '''
            }else {
                error "Node Not Listed in APT"
            }
        } else if (yumNodes.contains(nodeName)) {

            if(nodeName == "min-centos-7-x64" || nodeName == "min-ol-9-x64"){            
                sh '''
                    sudo yum install -y epel-release
                    sudo yum -y update
                    sudo yum install -y ansible git wget tar
                '''
            }else if(nodeName == "min-ol-8-x64"){
                sh '''
                    sudo yum install -y epel-release
                    sudo yum -y update
                    sudo yum install -y ansible-2.9.27 git wget tar
                '''
            }else {
                error "Node Not Listed in YUM"
            }
        } else {
            echo "Unexpected node name: ${nodeName}"
        }
    } catch (Exception e) {
        slackNotify("${SLACKNOTIFY}", "#FF0000", "[${JOB_NAME}]: Server Provision for Mini Package Testing for ${nodeName} at ${BRANCH}  FAILED !!")
    }

}

def runPlaybook(def nodeName) {
    
    def product_to_test = 'innovation-lts'
    def install_repo = 'testing'
    def action_to_test = 'install'
    def check_warnings = 'yes'
    def install_mysql_shell = 'no'

    try {
        def playbook = "ps_lts_innovation.yml"
        def playbook_path = "package-testing/playbooks/${playbook}"

        sh '''
            set -xe
            git clone -b POC-INNOVATION-LTS --depth 1 https://github.com/panchal-yash/package-testing
        '''
        sh """
            set -xe
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
    } catch (Exception e) {
        //slackNotify("${SLACKNOTIFY}", "#FF0000", "[${JOB_NAME}]: Mini Package Testing for ${nodeName} at ${BRANCH}  FAILED !!!")
        mini_test_error="True"
    }
}

def minitestNodes = [  "min-bullseye-x64",
                       "min-bookworm-x64",
                       "min-centos-7-x64",
                       "min-ol-8-x64",
                       "min-bionic-x64",
                       "min-focal-x64",
                       "min-jammy-x64",
                       "min-ol-9-x64"     ]

def package_tests_ps80(def nodes) {
    def stepsForParallel = [:]
    for (int i = 0; i < nodes.size(); i++) {
        def nodeName = nodes[i]
        stepsForParallel[nodeName] = {
            stage("Minitest run on ${nodeName}") {
                node(nodeName) {
                        installDependencies(nodeName)
                        runPlaybook(nodeName)
                }
            }
        }
    }
    parallel stepsForParallel
}

def AWS_STASH_PATH
@Field def PS8_RELEASE_VERSION
@Field def mini_test_error = "False"

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
        choice(
            choices: 'dev-server-qa\n#releases\n#releases-ci',
            description: 'Channel for notifications',
            name: 'SLACKNOTIFY')
    }
    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        timestamps ()
    }
    stages {
        stage('Create PS source tarball') {
            agent {
               label 'min-bionic-x64'
            }
            steps {
                cleanUpWS()
                installCli("deb")
                //buildStage("none", "--get_sources=1")

        
                sh '''
                   set -o xtrace
                   mkdir -p test
                   export build_dir=\$(pwd -P)
                   cd \${build_dir}
                   echo ${AWS_STASH_PATH} > awsUploadPath
                   cat <<EOF > test/percona-server-8.0.properties
MYSQL_VERSION_MAJOR=8
MYSQL_VERSION_MINOR=0
MYSQL_VERSION_PATCH=28
MYSQL_VERSION_EXTRA=-19

REVISION=5d19eae2493
BRANCH_NAME=release-8.0.28-19
PRODUCT=Percona-Server-8.0
PRODUCT_FULL=Percona-Server-8.0.28-19
BUILD_NUMBER=6
BUILD_ID=6
BUILD_TOKUDB_TOKUBACKUP=0
PERCONAFT_REPO=
PERCONAFT_BRANCH=Percona-Server-8.0.27-18
TOKUBACKUP_REPO=
TOKUBACKUP_BRANCH=Percona-Server-8.0.27-18
ZENFS_REPO=
ZENFS_BRANCH=
TOKUDB_VERSION=8.0.28-19
BOOST_PACKAGE_NAME=boost_1_73_0
RPM_RELEASE=1
DEB_RELEASE=1
DESTINATION=experimental
EOF
                   cat test/percona-server-8.0.properties
                '''
                stash includes: 'test/percona-server-8.0.properties', name: 'properties'
            }
        }
/*
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
                        buildStage("none", "--build_src_rpm=1")

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
                        buildStage("none", "--build_source_deb=1")

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
                        buildStage("none", "--build_rpm=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_rpm=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Centos 8 ARM') {
                    agent {
                        label 'docker-32gb-aarch64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("centos:8", "--build_rpm=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Oracle Linux 9') {
                    agent {
                        label 'min-ol-9-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("none", "--build_rpm=1 --with_zenfs=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Oracle Linux 9 ARM') {
                    agent {
                        label 'docker-32gb-aarch64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("oraclelinux:9", "--build_rpm=1")

                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_deb=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_deb=1 --with_zenfs=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Jammy(22.04)') {
                    agent {
                        label 'min-jammy-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("none", "--build_deb=1 --with_zenfs=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_deb=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_deb=1 --with_zenfs=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Debian Bookworm(12)') {
                    agent {
                        label 'min-bookworm-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("none", "--build_deb=1 --with_zenfs=1")

                        pushArtifactFolder("deb/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_tarball=1 ")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
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
                        buildStage("none", "--debug=1 --build_tarball=1 ")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_tarball=1 ")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
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
                        buildStage("none", "--debug=1 --build_tarball=1 ")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Oracle Linux 9 tarball') {
                    agent {
                        label 'min-ol-9-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--build_tarball=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Oracle Linux 9 ZenFS tarball') {
                    agent {
                        label 'min-ol-9-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--build_tarball=1 --with_zenfs=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Oracle Linux 9 debug tarball') {
                    agent {
                        label 'min-ol-9-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("rpm")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--debug=1 --build_tarball=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
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
                        buildStage("none", "--build_tarball=1 ")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
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
                        buildStage("none", "--debug=1 --build_tarball=1 ")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Focal(20.04) tarball') {
                    agent {
                        label 'min-focal-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--build_tarball=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Focal(20.04) debug tarball') {
                    agent {
                        label 'min-focal-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--debug=1 --build_tarball=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Jammy(22.04) tarball') {
                    agent {
                        label 'min-jammy-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--build_tarball=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Jammy(22.04) ZenFS tarball') {
                    agent {
                        label 'min-jammy-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--build_tarball=1 --with_zenfs=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Jammy(22.04) debug tarball') {
                    agent {
                        label 'min-jammy-x64'
                    }
                    steps {
                        cleanUpWS()
                        installCli("deb")
                        unstash 'properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("none", "--debug=1 --build_tarball=1")

                        pushArtifactFolder("tarball/", AWS_STASH_PATH)
                    }
                }
            }
        }
        stage('Upload packages and tarballs from S3') {
            agent {
                label 'min-jammy-x64'
            }
            steps {
                cleanUpWS()
                installCli("deb")
                unstash 'properties'

                uploadRPMfromAWS("rpm/", AWS_STASH_PATH)
                uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                uploadTarballfromAWS("tarball/", AWS_STASH_PATH, 'binary')
            }
        }
        stage('Sign packages') {
            steps {
                signRPM()
                signDEB()
            }
        }
        stage('Push to public repository') {
            steps {
                unstash 'properties'
                script {
                    PS_MAJOR_RELEASE = sh(returnStdout: true, script: ''' echo ${BRANCH} | sed "s/release-//g" | sed "s/\\.//g" | awk '{print substr($0, 0, 2)}' ''').trim()
                    // sync packages
                    sync2ProdAutoBuild("ps-"+PS_MAJOR_RELEASE, COMPONENT)
                }
            }
        }
        stage('Push Tarballs to TESTING download area') {
            steps {
                script {
                    try {
                        uploadTarballToDownloadsTesting("ps", "${BRANCH}")
                    }
                    catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
*/

    }

    post {
        success {
            //slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: build has been finished successfully for ${BRANCH} - [${BUILD_URL}]")
            //slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: Triggering Builds for Package Testing for ${BRANCH} - [${BUILD_URL}]")
            unstash 'properties'
            script {
                currentBuild.description = "Built on ${BRANCH}; path to packages: ${COMPONENT}/${AWS_STASH_PATH}"
                REVISION = sh(returnStdout: true, script: "grep REVISION test/percona-server-8.0.properties | awk -F '=' '{ print\$2 }'").trim()
                PS_RELEASE = sh(returnStdout: true, script: "echo ${BRANCH} | sed 's/release-//g'").trim()
                PS8_RELEASE_VERSION = sh(returnStdout: true, script: """ echo ${BRANCH} | sed -nE '/release-(8\\.[0-9]{1})\\..*/s//\\1/p' """).trim()
                
                
                if("${PS8_RELEASE_VERSION}"){
                    echo "Executing MINITESTS as VALID VALUES FOR PS8_RELEASE_VERSION:${PS8_RELEASE_VERSION}"
                    echo "Checking for the Github Repo VERSIONS file changes..."
                withCredentials([string(credentialsId: 'GITHUB_API_TOKEN', variable: 'TOKEN')]) {
                sh """
                    set -x
                    git clone -b testing-branch  https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git
                    cd package-testing
                    git config user.name "jenkins-pxc-cd"
                    git config user.email "it+jenkins-pxc-cd@percona.com"
                    echo "${PS8_RELEASE_VERSION} is the VALUE!!@!"
                    export RELEASE_VER_VAL="${PS8_RELEASE_VERSION}"
                    if [[ "\$RELEASE_VER_VAL" =~ ^8.[0-9]{1}\$ ]]; then
                        echo "\$RELEASE_VER_VAL is a valid version"
                        OLD_REV=\$(cat VERSIONS | grep PS_INN_LTS_REV | cut -d '=' -f2- )
                        OLD_VER=\$(cat VERSIONS | grep PS_INN_LTS_VER | cut -d '=' -f2- )
                        sed -i s/PS_INN_LTS_REV=\$OLD_REV/PS_INN_LTS_REV='"'${REVISION}'"'/g VERSIONS
                        sed -i s/PS_INN_LTS_VER=\$OLD_VER/PS_INN_LTS_VER='"'${PS_RELEASE}'"'/g VERSIONS

                    else
                        echo "INVALID PS8_RELEASE_VERSION VALUE: ${PS8_RELEASE_VERSION}"
                    fi
                    git diff
                    if [[ -z \$(git diff) ]]; then
                        echo "No changes"
                    else
                        echo "There are changes"
                        git add -A
                    git commit -m "Autocommit: add ${REVISION} and ${PS_RELEASE} for ps80 ${PS8_RELEASE_VERSION} package testing VERSIONS file."
                        git push
                    fi
                """
                }
                    package_tests_ps80(minitestNodes)
                    if("${mini_test_error}" == "True"){
                        error "NOT TRIGGERING PACKAGE TESTS AND INTEGRATION TESTS DUE TO MINITEST FAILURE !!"
                    }else{
                        echo "TRIGGERING THE PACKAGE TESTING JOB!!!"
                        //build job: 'package-testing-ps-innovation-lts', propagate: false, wait: false, parameters: [string(name: 'product_to_test', value: "${product_to_test}"),string(name: 'install_repo', value: "testing"),string(name: 'node_to_test', value: "all"),string(name: 'action_to_test', value: "all"),string(name: 'check_warnings', value: "yes"),string(name: 'install_mysql_shell', value: "no")]
                                                                                                                                            
                        echo "Trigger PMM_PS Github Actions Workflow"
/*                        
                        withCredentials([string(credentialsId: 'GITHUB_API_TOKEN', variable: 'GITHUB_API_TOKEN')]) {
                            sh """
                                curl -i -v -X POST \
                                    -H "Accept: application/vnd.github.v3+json" \
                                    -H "Authorization: token ${GITHUB_API_TOKEN}" \
                                    "https://api.github.com/repos/Percona-Lab/qa-integration/actions/workflows/PMM_PS.yaml/dispatches" \
                                    -d '{"ref":"main","inputs":{"ps_version":"${PS_RELEASE}"}}'
                            """
                        }
*/
                    }
                }
                else{
                    error "Skipping MINITESTS and Other Triggers as invalid RELEASE VERSION FOR THIS JOB"
                    //slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: Skipping MINITESTS and Other Triggers as invalid RELEASE VERSION FOR THIS JOB ${BRANCH} - [${BUILD_URL}]")
                } 
            }
            deleteDir()
        }
        failure {
            //slackNotify("${SLACKNOTIFY}", "#FF0000", "[${JOB_NAME}]: build failed for ${BRANCH} - [${BUILD_URL}]")
            script {
                currentBuild.description = "Built on ${BRANCH}"
            }
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
