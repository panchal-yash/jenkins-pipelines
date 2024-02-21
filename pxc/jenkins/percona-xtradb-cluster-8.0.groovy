library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

import groovy.transform.Field

void buildStage(String DOCKER_OS, String STAGE_PARAM) {
    sh """
        set -o xtrace
        mkdir -p test
        wget \$(echo ${GIT_REPO} | sed -re 's|github.com|raw.githubusercontent.com|; s|\\.git\$||')/${GIT_BRANCH}/build-ps/pxc_builder.sh -O pxc_builder.sh
        pwd -P
        ls -laR
        export build_dir=\$(pwd -P)
        docker run -u root -v \${build_dir}:\${build_dir} ${DOCKER_OS} sh -c "
            set -o xtrace
            cd \${build_dir}
            bash -x ./pxc_builder.sh --builddir=\${build_dir}/test --install_deps=1
            bash -x ./pxc_builder.sh --builddir=\${build_dir}/test --repo=${GIT_REPO} --branch=${GIT_BRANCH} --rpm_release=${RPM_RELEASE} --deb_release=${DEB_RELEASE} --bin_release=${BIN_RELEASE} ${STAGE_PARAM}"
    """
}

void cleanUpWS() {
    sh """
        sudo rm -rf ./*
    """
}

def installDependencies(def nodeName) {
    def aptNodes = ['min-bullseye-x64', 'min-bookworm-x64', 'min-focal-x64', 'min-jammy-x64', 'min-buster-x64']
    def yumNodes = ['min-ol-8-x64', 'min-centos-7-x64', 'min-ol-9-x64', 'min-amazon-2-x64']
    try{
        if (aptNodes.contains(nodeName)) {
            if(nodeName == "min-bullseye-x64" || nodeName == "min-bookworm-x64" || nodeName == "min-buster-x64"){            
                sh '''
                    sudo apt-get update
                    sudo apt-get install -y ansible git wget
                '''
            }else if(nodeName == "min-focal-x64" || nodeName == "min-jammy-x64"){
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
            }else if(nodeName == "min-amazon-2-x64"){
                sh '''
                    sudo amazon-linux-extras install epel
                    sudo yum -y update
                    sudo yum install -y ansible git wget
                '''
            }
            else {
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

    try {
        sh """
            set -xe
            echo "Starting things.."
            touch /tmp/envfile
            echo "install_repo: \"\${COMPONENT}\"" > "/tmp/envfile"
            cat /tmp/envfile
            sudo mkdir /mainpath
            cd /mainpath            
            sudo git clone --depth 1 -b pxc-innovation-lts-package-testing https://github.com/panchal-yash/package-testing.git
            sudo wget https://raw.githubusercontent.com/panchal-yash/package-testing/pxc-innovation-lts-package-testing/ansible-utils.sh -O ansible-utils.sh
            sudo chmod +x ansible-utils.sh
            sudo ./ansible-utils.sh task2playbook /mainpath/package-testing/molecule/pxc/pxc-innovation-lts-bootstrap-install/tasks/main.yml tasks2play.yml
            sudo ./ansible-utils.sh taskcomment tasks2play.yml "Fetch the vars from file for install"
            sudo ./ansible-utils.sh taskcomment tasks2play.yml "copy pxc config on centos"
            sudo ./ansible-utils.sh taskcomment tasks2play.yml "configure PXC on debian/ubuntu"
            sudo ./ansible-utils.sh taskcomment tasks2play.yml "copy .my.cnf with credentials on centos"
            sudo mv tasks2play.yml /mainpath/package-testing/molecule/pxc/pxc-innovation-lts-bootstrap-install/tasks/
            sudo sed -i "s/hosts: all/hosts: localhost/g" /mainpath/package-testing/molecule/pxc/playbooks/prepare.yml
            cd /mainpath/package-testing/molecule/pxc/playbooks/
            sudo MOLECULE_ENV_FILE="/tmp/envfile" ansible-playbook -vvvv prepare.yml
            cd /mainpath/package-testing/molecule/pxc/pxc-innovation-lts-bootstrap-install/tasks/
            cat tasks2play.yml
            sudo install_repo="\${COMPONENT}" ansible-playbook -vvvv tasks2play.yml
        """
    } catch (Exception e) {
        slackNotify("${SLACKNOTIFY}", "#FF0000", "[${JOB_NAME}]: Mini Package Testing for ${nodeName} at ${BRANCH}  FAILED !!!")
        mini_test_error="True"
    }
}

def minitestNodes = [  "min-bullseye-x64",
                       "min-bookworm-x64",
                       "min-buster-x64",
                       "min-centos-7-x64",
                       "min-ol-8-x64",
                       "min-focal-x64",
                       "min-amazon-2-x64",
                       "min-jammy-x64",
                       "min-ol-9-x64"     ]

def package_tests_pxc(def nodes) {
    def stepsForParallel = [:]
    for (int i = 0; i < nodes.size(); i++) {
        def nodeName = nodes[i]
        stepsForParallel[nodeName] = {
            stage("Minitest run on ${nodeName}") {
                node(nodeName) {
                        installDependencies(nodeName)
                        echo "Start sp,etjomg"
                        runPlaybook(nodeName)
                }
            }
        }
    }
    parallel stepsForParallel
}

@Field def mini_test_error = "False"
def AWS_STASH_PATH
def PS8_RELEASE_VERSION
def product_to_test = 'innovation-lts'
def install_repo = "${COMPONENT}"
def action_to_test = 'install'
def check_warnings = 'yes'
def install_mysql_shell = 'no'

pipeline {
    agent {
        label 'docker-32gb'
    }
    parameters {
        string(
            defaultValue: 'https://github.com/percona/percona-xtradb-cluster.git',
            description: 'URL for percona-xtradb-cluster repository',
            name: 'GIT_REPO')
        string(
            defaultValue: '8.0',
            description: 'Tag/Branch for percona-xtradb-cluster repository',
            name: 'GIT_BRANCH')
        string(
            defaultValue: '1',
            description: 'RPM release value',
            name: 'RPM_RELEASE')
        string(
            defaultValue: '1',
            description: 'DEB release value',
            name: 'DEB_RELEASE')
        string(
            defaultValue: '1',
            description: 'BIN release value',
            name: 'BIN_RELEASE')
        choice(
            choices: 'pxc-80\npxc-8x-innovation',
            description: 'PXC repo name',
            name: 'PXC_REPO')
        choice(
            choices: 'laboratory\ntesting\nexperimental',
            description: 'Repo component to push packages to',
            name: 'COMPONENT')
        choice(
            choices: '#dev-server-qa\n#releases\n#releases-ci',
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

/*
                        stage("CREATE PROP"){
                    steps{
                        script {
                            sh """
                                mkdir test 
                                cd test
cat <<EOF > pxc-80.properties
WSREP_VERSION=26.1.4.3
WSREP_REV=b871d7e
REVISION=84d9464
MYSQL_VERSION=8.1.0-1
MYSQL_RELEASE=27
BRANCH_NAME=release-8.1.0-1
PRODUCT=Percona-XtraDB-Cluster-80
PRODUCT_FULL=Percona-XtraDB-Cluster-8.1.0-1
BUILD_NUMBER=
DESTINATION=UPLOAD/UPLOAD/experimental/BUILDS/Percona-XtraDB-Cluster-80/Percona-XtraDB-Cluster-8.0.35/release-8.0.35/84d9464//BUILDS/Percona-XtraDB-Cluster-80/Percona-XtraDB-Cluster-8.0.35/release-8.0.35/84d9464/
GALERA_REVNO=b73532f
DEST=UPLOAD/experimental/BUILDS/Percona-XtraDB-Cluster-80/Percona-XtraDB-Cluster-8.0.35/release-8.0.35/84d9464/
EOF

                            """
                            stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        }
                    }

                }

*/
        stage('Create PXC source tarball') {
            steps {
                slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: starting build for ${GIT_BRANCH} - [${BUILD_URL}]")
                cleanUpWS()
                buildStage("centos:7", "--get_sources=1")
                sh '''
                   REPO_UPLOAD_PATH=$(grep "DEST=UPLOAD" test/pxc-80.properties | cut -d = -f 2 | sed "s:$:${BUILD_NUMBER}:")
                   AWS_STASH_PATH=$(echo ${REPO_UPLOAD_PATH} | sed  "s:UPLOAD/experimental/::")
                   echo ${REPO_UPLOAD_PATH} > uploadPath
                   echo ${AWS_STASH_PATH} > awsUploadPath
                   cat test/pxc-80.properties
                   cat uploadPath
                   cat awsUploadPath
                '''
                script {
                    AWS_STASH_PATH = sh(returnStdout: true, script: "cat awsUploadPath").trim()
                }
                stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                stash includes: 'uploadPath', name: 'uploadPath'
                //pushArtifactFolder("source_tarball/", AWS_STASH_PATH)
                //uploadTarballfromAWS("source_tarball/", AWS_STASH_PATH, 'source')
            }
        }
        
        /*
        stage('Build PXC generic source packages') {
            parallel {
                stage('Build PXC generic source rpm') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_src_rpm=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("srpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("srpm/", AWS_STASH_PATH)
                    }
                }
                stage('Build PXC generic source deb') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("ubuntu:xenial", "--build_source_deb=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("source_deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("source_deb/", AWS_STASH_PATH)
                    }
                }

            }  //parallel
        } // stage
    /*
        stage('Build PXC RPMs/DEBs/Binary tarballs') {
            parallel {
                stage('Centos 7') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_rpm=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Centos 8') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("centos:8", "--build_rpm=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Oracle Linux 9') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("srpm/", AWS_STASH_PATH)
                        buildStage("oraclelinux:9", "--build_rpm=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("rpm/", AWS_STASH_PATH)
                        uploadRPMfromAWS("rpm/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Focal(20.04)') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("ubuntu:focal", "--build_deb=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Ubuntu Jammy(22.04)') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("ubuntu:jammy", "--build_deb=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Debian Buster(10)') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("debian:buster", "--build_deb=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Debian Bullseye(11)') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("debian:bullseye", "--build_deb=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Debian Bookworm(12)') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_deb/", AWS_STASH_PATH)
                        buildStage("debian:bookworm", "--build_deb=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("deb/", AWS_STASH_PATH)
                        uploadDEBfromAWS("deb/", AWS_STASH_PATH)
                    }
                }
                stage('Centos 7 tarball') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_tarball=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("test/tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("test/tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Centos 7 debug tarball') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("centos:7", "--build_tarball=1 --debug=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("debug/", AWS_STASH_PATH)
                    }
                }
                stage('Centos 9 tarball') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("oraclelinux:9", "--build_tarball=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("test/tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("test/tarball/", AWS_STASH_PATH, 'binary')
                    }
                }
                stage('Ubuntu Jammy(22.04) tarball') {
                    agent {
                        label 'docker-32gb'
                    }
                    steps {
                        cleanUpWS()
                        unstash 'pxc-80.properties'
                        popArtifactFolder("source_tarball/", AWS_STASH_PATH)
                        buildStage("ubuntu:jammy", "--build_tarball=1")

                        stash includes: 'test/pxc-80.properties', name: 'pxc-80.properties'
                        pushArtifactFolder("test/tarball/", AWS_STASH_PATH)
                        uploadTarballfromAWS("test/tarball/", AWS_STASH_PATH, 'binary')
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
        stage('Push to public repository') {
            steps {
                script {
                    PXC_VERSION_MINOR = sh(returnStdout: true, script: ''' curl -s -O $(echo ${GIT_REPO} | sed -re 's|github.com|raw.githubusercontent.com|; s|\\.git$||')/${GIT_BRANCH}/MYSQL_VERSION; cat MYSQL_VERSION | grep MYSQL_VERSION_MINOR | awk -F= '{print $2}' ''').trim()
                    if ("${PXC_VERSION_MINOR}" == "0") {
                    // sync packages
                        sync2ProdAutoBuild(PXC_REPO, COMPONENT)
                    } else {
                        sync2ProdAutoBuild("pxc-8x-innovation", COMPONENT)
                    }
                }
            }
        }
        stage('Push Tarballs to TESTING download area') {
            steps {
                script {
                    try {
                        uploadTarballToDownloadsTesting("pxc", "${GIT_BRANCH}")
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
            //slackNotify("#00FF00", "[${JOB_NAME}]: build has been finished successfully for ${GIT_BRANCH} - [${BUILD_URL}]")

            unstash 'pxc-80.properties'
            script {
                currentBuild.description = "Built on ${GIT_BRANCH}; path to packages: ${COMPONENT}/${AWS_STASH_PATH}"
                GALERA_REVNO = sh(returnStdout: true, script: "grep GALERA_REVNO test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                PXC_RELEASE = sh(returnStdout: true, script: "echo ${GIT_BRANCH} | sed 's/release-//g'").trim()
                PXC_RELEASE_VERSION = sh(returnStdout: true, script: """ echo ${GIT_BRANCH} | sed -nE '/release-(8\\.[0-9]{1})\\..*/s//\\1/p' """).trim()
                

                if("${PXC_RELEASE_VERSION}"){
                    echo "Executing MINITESTS as VALID VALUES FOR PXC_RELEASE_VERSION:${PXC_RELEASE_VERSION}"

                    sh """
                    wget https://raw.githubusercontent.com/percona/galera/${GALERA_REVNO}/GALERA_VERSION -O GALERA_VERSION
                    wget https://raw.githubusercontent.com/percona/percona-xtradb-cluster/${GIT_BRANCH}/storage/innobase/include/univ.i -O univ.i

                    PERCONA_INNODB_VERSION=\$(grep "#define PERCONA_INNODB_VERSION" univ.i | awk '{ print \$3 }')
                    GALERA_VERSION_MAJOR=\$(grep GALERA_VERSION_MAJOR GALERA_VERSION | cut -d = -f2-)
                    GALERA_VERSION_MINOR=\$(grep GALERA_VERSION_MINOR GALERA_VERSION | cut -d = -f2-)

                    echo "GALERA_VERSION_MAJOR=\$GALERA_VERSION_MAJOR" >> test/pxc-80.properties
                    echo "GALERA_VERSION_MINOR=\$GALERA_VERSION_MINOR" >> test/pxc-80.properties
                    echo "PERCONA_INNODB_VERSION=\$PERCONA_INNODB_VERSION" >> test/pxc-80.properties

                    cat test/pxc-80.properties
                    """

                    GALERA_VERSION_MAJOR = sh(returnStdout: true, script: "grep GALERA_VERSION_MAJOR test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                    GALERA_VERSION_MINOR = sh(returnStdout: true, script: "grep GALERA_VERSION_MINOR test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                    MYSQL_VERSION = sh(returnStdout: true, script: "grep MYSQL_VERSION test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                    MYSQL_RELEASE = sh(returnStdout: true, script: "grep MYSQL_RELEASE test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                    REVISION = sh(returnStdout: true, script: "grep REVISION test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                    PXC_INN_LTS_INNODB = sh(returnStdout: true, script: "grep PERCONA_INNODB_VERSION test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()
                    //PERCONA_INNODB_VERSION = sh(returnStdout: true, script: "grep PERCONA_INNODB_VERSION test/pxc-80.properties | awk -F '=' '{ print\$2 }'").trim()

                    echo "Checking for the Github Repo VERSIONS file changes..."
                    withCredentials([string(credentialsId: 'GITHUB_API_TOKEN', variable: 'TOKEN')]) {
                    sh """
                        set -x
                        git clone -b testing-branch https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git
                        cd package-testing
                        git config user.name "jenkins-pxc-cd"
                        git config user.email "it+jenkins-pxc-cd@percona.com"
                        echo "${PXC_RELEASE_VERSION} is the VALUE!!@!"
                        export RELEASE_VER_VAL="${PXC_RELEASE_VERSION}"

                        if [[ "\$RELEASE_VER_VAL" =~ ^8.[0-9]{1}\$ ]]; then
                            echo "\$RELEASE_VER_VAL is a valid version"
                            OLD_REV=\$(cat VERSIONS | grep PS_INN_LTS_REV | cut -d '=' -f2- )
                            PXC_INN_LTS_REPO="pxc-8x-innovation"
                            OLD_PXC_INN_LTS_VER=\$(cat VERSIONS | grep PXC_INN_LTS_VER | cut -d '=' -f2- )
                            OLD_PXC_INN_LTS_WSREP=\$(cat VERSIONS | grep PXC_INN_LTS_WSREP | cut -d '=' -f2- )
                            OLD_REVISION=\$(cat VERSIONS | grep PXC_INN_LTS_REV | cut -d '=' -f2- )
                            OLD_PXC_INN_LTS_REPO=\$(cat VERSIONS | grep PXC_INN_LTS_REPO | cut -d '=' -f2- )
                            OLD_PXC_INN_LTS_INNODB=\$(cat VERSIONS | grep PXC_INN_LTS_INNODB | cut -d '=' -f2- )

                            sed -i s/PXC_INN_LTS_VER=\$OLD_PXC_INN_LTS_VER/PXC_INN_LTS_VER='"'${MYSQL_VERSION}-${MYSQL_RELEASE}'"'/g VERSIONS
                            sed -i s/PXC_INN_LTS_WSREP=\$OLD_PXC_INN_LTS_WSREP/PXC_INN_LTS_WSREP='"'${GALERA_VERSION_MAJOR}.${GALERA_VERSION_MINOR}'('${GALERA_REVNO}')"'/g VERSIONS
                            sed -i s/PXC_INN_LTS_REV=\$OLD_REVISION/PXC_INN_LTS_REV='"'${REVISION}'"'/g VERSIONS
                            sed -i s/PXC_INN_LTS_REPO=\$OLD_PXC_INN_LTS_REPO/PXC_INN_LTS_REPO='"'\$PXC_INN_LTS_REPO'"'/g VERSIONS
                            sed -i s/PXC_INN_LTS_INNODB=\$OLD_PXC_INN_LTS_INNODB/PXC_INN_LTS_INNODB='"'${PXC_INN_LTS_INNODB}'"'/g VERSIONS
                            
                        else
                            echo "INVALID PXC_RELEASE_VERSION VALUE: ${PXC_RELEASE_VERSION}"
                        fi
                        git diff
                        if [[ -z \$(git diff) ]]; then
                            echo "No changes"
                        else
                            echo "There are changes"
                            git add -A
                        git commit -m "Autocommit: add ${REVISION} and ${PXC_RELEASE} for ${PXC_RELEASE_VERSION} package testing VERSIONS file."
                            git push
                        fi
                    """
                    }
                    echo "Start Minitests for PXC"          

                    
                    package_tests_pxc(minitestNodes)

                    if("${mini_test_error}" == "True"){
                        error "NOT TRIGGERING PACKAGE TESTS AND INTEGRATION TESTS DUE TO MINITEST FAILURE !!"
                    }else{
                        echo "TRIGGERING THE PACKAGE TESTING JOB!!!"
                        build job: 'package-testing-ps-innovation-lts', propagate: false, wait: false, parameters: [string(name: 'product_to_test', value: "${product_to_test}"),string(name: 'install_repo', value: "testing"),string(name: 'node_to_test', value: "all"),string(name: 'action_to_test', value: "all"),string(name: 'check_warnings', value: "yes"),string(name: 'install_mysql_shell', value: "no")]
                                                                                                                                            
                        echo "Trigger PMM_PS Github Actions Workflow"
                        
                        withCredentials([string(credentialsId: 'GITHUB_API_TOKEN', variable: 'GITHUB_API_TOKEN')]) {
                            sh """
                                curl -i -v -X POST \
                                    -H "Accept: application/vnd.github.v3+json" \
                                    -H "Authorization: token ${GITHUB_API_TOKEN}" \
                                    "https://api.github.com/repos/Percona-Lab/qa-integration/actions/workflows/PMM_PS.yaml/dispatches" \
                                    -d '{"ref":"main","inputs":{"ps_version":"${PS_RELEASE}"}}'
                            """
                        }

                    }
                }
                else{
                    error "Skipping MINITESTS and Other Triggers as invalid RELEASE VERSION FOR THIS JOB"
                    //slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: Skipping MINITESTS and Other Triggers as invalid RELEASE VERSION FOR THIS JOB ${GIT_BRANCH} - [${BUILD_URL}]")
                }
            }
            deleteDir()
        }
        failure {
            //slackNotify("${SLACKNOTIFY}", "#FF0000", "[${JOB_NAME}]: build failed for ${GIT_BRANCH} - [${BUILD_URL}]")
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
