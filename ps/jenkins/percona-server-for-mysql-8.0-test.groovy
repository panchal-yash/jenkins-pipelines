/* groovylint-disable DuplicateStringLiteral, GStringExpressionWithinString, LineLength */
library changelog: false, identifier: 'lib@master', retriever: modernSCM([
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

def AWS_STASH_PATH

def package_tests_ps80(){

                    ps80_install_pkg_minitests_playbook = 'ps_80.yml'
                    install_repo = 'testing'
                    action_to_test = 'install'
                    check_warnings = 'no'
                    install_mysql_shell = 'no'
                    def arrayA = [  "min-buster-x64",
                                    "min-bullseye-x64",
                                    "min-bookworm-x64",
                                    "min-centos-7-x64",
                                    "min-ol-8-x64",
                                    "min-bionic-x64",
                                    "min-focal-x64",
                                    //"min-amazon-2-x64",
                                    "min-jammy-x64",
                                    "min-ol-9-x64"     ]

                    def stepsForParallel = [:]

                    for (int i = 0; i < arrayA.size(); i++) {
                        def nodeName = arrayA[i]
                        stepsForParallel[nodeName] = {
                                stage("Run on ${nodeName}") {
                                    node(nodeName){
                                    try{
                                        if (nodeName == 'min-buster-x64' || nodeName == 'min-bullseye-x64' || nodeName == 'min-bookworm-x64') {
                                            
                                            sh '''
                                                sudo apt-get update
                                                sudo apt-get install -y ansible git wget
                                            '''

                                        } else if (nodeName == 'min-ol-8-x64') {
                                            
                                            error ("FAILED ON OL8")
//                                            sh '''
//                                                sudo yum install -y epel-release
//                                                sudo yum -y update
//                                                sudo yum install -y ansible-2.9.27 git wget tar
//                                            '''

                                        } else if (nodeName == 'min-centos-7-x64' || nodeName == 'min-ol-9-x64'){
                                            
                                            sh '''
                                                sudo yum install -y epel-release
                                                sudo yum -y update
                                                sudo yum install -y ansible git wget tar
                                            '''

                                        } else if (nodeName == 'min-bionic-x64' || nodeName == 'min-focal-x64' || nodeName == 'min-jammy-x64'){

                                            sh '''
                                                sudo apt-get update
                                                sudo apt-get install -y software-properties-common
                                                sudo apt-add-repository --yes --update ppa:ansible/ansible
                                                sudo apt-get install -y ansible git wget
                                            '''
                                        
                                        } else if (nodeName == 'min-amazon-2-x64'){

                                            sh '''
                                                sudo amazon-linux-extras install epel
                                                sudo yum -y update
                                                sudo yum install -y ansible git wget
                                            '''

                                        }  else {
                                            
                                            echo "Unexpected node name: ${nodeName}"
                                        
                                        }
                                    } catch (Exception e){
                                            stageSuccess = false
                                            slackNotify("#dev-server-qa", "#FF0000", "[${JOB_NAME}]: Server Provision for Mini Package Testing for ${nodeName} at ${BRANCH}  FAILED !!")
                                    }
                                        def playbook = "${ps80_install_pkg_minitests_playbook}"
                                        def playbook_path = "package-testing/playbooks/${playbook}"

                                        sh '''
                                            git clone --depth 1 https://github.com/Percona-QA/package-testing
                                        '''

                                        try{
                                            error "EROOR HERE"
//                                            sh """
//                                                export install_repo="\${install_repo}"
//                                                export client_to_test="ps80"
//                                                export check_warning="\${check_warnings}"
//                                                export install_mysql_shell="\${install_mysql_shell}"
//                                                ansible-playbook \
//                                                --connection=local \
//                                                --inventory 127.0.0.1, \
//                                                --limit 127.0.0.1 \
//                                                ${playbook_path}
//                                            """
                                            echo "${playbook_path}"
                                        } catch (Exception e){
                                            stageSuccess = false
                                            slackNotify("#dev-server-qa", "#FF0000", "[${JOB_NAME}]: Mini Package Testing for ${nodeName} at ${BRANCH}  FAILED !!")
                                        }
                                        if (!stageSuccessful) {
                                            error("Mini Package Tests Failed! for ${nodeName}")
                                        }
                                    }                                    
                                }
                        }
                    }
                    parallel stepsForParallel

}


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
            choices: '#releases\n#releases-ci',
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
                echo "Stage 1"
            }
        }
    }
    post {
        success {
//            slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: build has been finished successfully for ${BRANCH} - [${BUILD_URL}]")
//            slackNotify("${SLACKNOTIFY}", "#00FF00", "[${JOB_NAME}]: Triggering Builds for Package Testing for ${BRANCH} - [${BUILD_URL}]")
//            unstash 'properties'
            echo "Notification Part"
            script {
//                currentBuild.description = "Built on ${BRANCH}; path to packages: ${COMPONENT}/${AWS_STASH_PATH}"
//                REVISION = sh(returnStdout: true, script: "grep REVISION test/percona-server-8.0.properties | awk -F '=' '{ print\$2 }'").trim()
//                PS_RELEASE = sh(returnStdout: true, script: "echo ${BRANCH} | sed 's/release-//g'").trim()
//
//                withCredentials([string(credentialsId: 'PXC_GITHUB_API_TOKEN', variable: 'TOKEN')]) {
//                sh """
//                    
//                    set -x
//                    git clone https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git
//                    cd package-testing
//                    git config user.name "jenkins-pxc-cd"
//                    git config user.email "it+jenkins-pxc-cd@percona.com"
//                    OLD_REV=\$(cat VERSIONS | grep PS80_REV | cut -d '=' -f2- )
//                    OLD_VER=\$(cat VERSIONS | grep PS80_VER | cut -d '=' -f2- )
//                    sed -i s/PS80_REV=\$OLD_REV/PS80_REV='"'${REVISION}'"'/g VERSIONS
//                    sed -i s/PS80_VER=\$OLD_VER/PS80_VER='"'${PS_RELEASE}'"'/g VERSIONS
//                    git diff
//                    git add -A
//                    git commit -m "Autocommit: add ${REVISION} and ${PS_RELEASE} for ps80 package testing VERSIONS file."
//                    git push 
//
//                """
//                }
//                echo "Trigger Package Testing Job for PS"
//                build job: 'package-testing-ps80', propagate: false, wait: false, parameters: [string(name: 'product_to_test', value: 'ps80'),string(name: 'install_repo', value: "testing"),string(name: 'node_to_test', value: "all"),string(name: 'action_to_test', value: "all"),string(name: 'check_warnings', value: "yes"),string(name: 'install_mysql_shell', value: "no")]
//                echo "Trigger PMM_PS Github Actions Workflow"
//                withCredentials([string(credentialsId: 'GITHUB_API_TOKEN', variable: 'GITHUB_API_TOKEN')]) {
//                    sh """
//                        curl -i -v -X POST \
//                             -H "Accept: application/vnd.github.v3+json" \
//                             -H "Authorization: token ${GITHUB_API_TOKEN}" \
//                             "https://api.github.com/repos/Percona-Lab/qa-integration/actions/workflows/PMM_PS.yaml/dispatches" \
//                             -d '{"ref":"main","inputs":{"ps_version":"${PS_RELEASE}"}}'
//                    """
//                }
                    package_tests_ps80()
                    echo "Post MINI TSTS PART"

             }
            deleteDir()
        }
        failure {
            slackNotify("#dev-server-qa", "#FF0000", "[${JOB_NAME}]: build failed for ${BRANCH} - [${BUILD_URL}]")
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