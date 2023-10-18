library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

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
                                    
                                        if (nodeName == 'min-buster-x64' || nodeName == 'min-bullseye-x64' || nodeName == 'min-bookworm-x64') {
                                            
                                            sh '''
                                                sudo apt-get update
                                                sudo apt-get install -y ansible git wget
                                            '''

                                        } else if (nodeName == 'min-ol-8-x64') {
                                            
                                            sh '''
                                                sudo yum install -y epel-release
                                                sudo yum -y update
                                                sudo yum install -y ansible-2.9.27 git wget tar
                                            '''

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

                                        def playbook = "${ps80_install_pkg_minitests_playbook}"
                                        def playbook_path = "package-testing/playbooks/${playbook}"

                                        sh '''
                                            git clone --depth 1 https://github.com/Percona-QA/package-testing
                                        '''

                                        try{
                                            sh "ERROR"
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
                                        } catch (Exception e){
                                            stageSuccess = false
                                            def BRANCH = "BRANCH"
                                            def BUILD_URL = "BUILD_URL"
                                            slackNotify("#dev-server-qa", "#FF0000", "[${JOB_NAME}]: Mini Package Testing for ${nodeName} at ${BRANCH} - [${BUILD_URL}] FAILED !  !")
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
    agent none

    stages {
        stage("Prepare") {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}"
                    currentBuild.description = "Testing.."
                }
                
            }
        }

        stage('Test Installation of the PS80 Testing Package') {
            steps 
            {
                script {
                    package_tests_ps80()
                }
            }
        }


    }

}
