library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

def runMoleculeAction(String action, String product_to_test, String scenario, String param_test_type, String test_repo, String version_check) {
    def awsCredentials = [
        sshUserPrivateKey(
            credentialsId: 'MOLECULE_AWS_PRIVATE_KEY',
            keyFileVariable: 'MOLECULE_AWS_PRIVATE_KEY',
            passphraseVariable: '',
            usernameVariable: ''
        ),
        aws(
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            credentialsId: '5d78d9c7-2188-4b16-8e31-4d5782c6ceaa',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        )
    ]

            sh """
            mkdir -p "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/"
            """


                if(action != "create" && action != "destroy"){
                    def IN_PXC1_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[0] | jq [.private_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def IN_PXC2_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[1] | jq [.private_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def IN_PXC3_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[2] | jq [.private_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    sh """
                        echo 'PXC1_IP: "${IN_PXC1_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                        echo 'PXC2_IP: "${IN_PXC2_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                        echo 'PXC3_IP: "${IN_PXC3_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                    """
                }

    withCredentials(awsCredentials) {

            if(action == "create" || action == "destroy"){
                sh"""
                    . virtenv/bin/activate
                    
                    
                    mkdir -p ${WORKSPACE}/install
                    mkdir -p ${WORKSPACE}/upgrade
                    
                    cd package-testing/molecule/pxc-keyring-test
                    
                    echo "param_test_type is ${param_test_type}"

                    cd pxc-80-setup
                    molecule ${action} -s ${scenario}
                    cd -

                    cd pxc-80-setup
                    molecule ${action} -s ${scenario}
                    cd -
                """
            }else{

                sh"""
                    . virtenv/bin/activate
                    cd package-testing/molecule/pxc-keyring-test

                    echo "param_test_type is ${param_test_type}"

                    cd pxc-80-setup
                    molecule -e ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile  ${action} -s ${scenario}
                    cd -
                """
            }
    }
}


void setInventories(String param_test_type){

                    def KEYPATH_BOOTSTRAP
                    def KEYPATH_COMMON
                    def SSH_USER

                    KEYPATH_COMMON="/home/centos/.cache/molecule/pxc-80-setup/${params.node_to_test}/ssh_key-us-west-2"
                    SSH_USER="ubuntu"            


                    echo "${SSH_USER}"
                    echo "${KEYPATH_BOOTSTRAP}"
                    echo "${KEYPATH_COMMON}"

                    def INSTALL_Common_Instance_PXC1 = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC1_Public_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC2 = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC2_Public_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC3 = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[2] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC3_Public_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[2] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    sh """

                        mkdir -p "${WORKSPACE}/pxc-80-setup/${params.node_to_test}/install/"
                        echo \"printing path of common  ${KEYPATH_COMMON}"
                        echo \"printing user ${SSH_USER}"
                        echo "\n ${INSTALL_Common_Instance_PXC1} ansible_host=${INSTALL_Common_Instance_PXC1_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${INSTALL_Common_Instance_PXC1}" > ${WORKSPACE}/pxc-80-setup/${params.node_to_test}/install/inventory            
                        echo "\n ${INSTALL_Common_Instance_PXC2} ansible_host=${INSTALL_Common_Instance_PXC2_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${INSTALL_Common_Instance_PXC2}" >> ${WORKSPACE}/pxc-80-setup/${params.node_to_test}/install/inventory
                        echo "\n ${INSTALL_Common_Instance_PXC3} ansible_host=${INSTALL_Common_Instance_PXC3_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${INSTALL_Common_Instance_PXC3}" >> ${WORKSPACE}/pxc-80-setup/${params.node_to_test}/install/inventory
                    """
}

void runlogsbackup(String product_to_test, String param_test_type) {
    def awsCredentials = [
        sshUserPrivateKey(
            credentialsId: 'MOLECULE_AWS_PRIVATE_KEY',
            keyFileVariable: 'MOLECULE_AWS_PRIVATE_KEY',
            passphraseVariable: '',
            usernameVariable: ''
        ),
        aws(
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            credentialsId: '5d78d9c7-2188-4b16-8e31-4d5782c6ceaa',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        )
    ]

                    def IN_PXC1_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[0] | jq [.private_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def IN_PXC2_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[1] | jq [.private_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def IN_PXC3_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[2] | jq [.private_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    sh """
                        echo 'PXC1_IP: "${IN_PXC1_IP}"' > "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                        echo 'PXC2_IP: "${IN_PXC2_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                        echo 'PXC3_IP: "${IN_PXC3_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                    """

    withCredentials(awsCredentials) {
        
        sh """
            . virtenv/bin/activate
            echo "Running the logs backup task for pxc common node"
            ansible-playbook ${WORKSPACE}/package-testing/molecule/pxc/playbooks/logsbackup.yml -i  ${WORKSPACE}/pxc-80-setup/${params.node_to_test}/${param_test_type}/inventory -e @${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile
        """
    }
    

    
}


pipeline {
    agent {
        label 'min-centos-7-x64'
    }

    options {
        skipDefaultCheckout()
    }

    environment {

        INSTALL_COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/install/common_instance_private_ip.json"
        INSTALL_COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/install/common_instance_public_ip.json"

        JENWORKSPACE = "${env.WORKSPACE}"

        DESTROY_ENV = "no"
    }

    parameters {
        choice(
            name: 'product_to_test',
            choices: [
                'pxc80'
            ],
            description: 'PXC product_to_test to test'
        )
        choice(
            name: 'node_to_test',
            choices: [
                'ubuntu-focal'
            ],
            description: 'Distribution to run test'
        )
        choice(
            name: 'test_type',
            choices: [
                'install'
            ],
            description: 'Test type to run test'
        )
    }

    stages {
        stage("Cleanup Workspace") {
            steps {                
                sh "sudo rm -rf ${WORKSPACE}/*"
            }
        }

        stage("Set up") {
            steps {             
                script{
                    currentBuild.displayName = "${env.BUILD_NUMBER}-${params.product_to_test}-${params.node_to_test}-${params.test_repo}-${params.test_type}"                    
                    if (( params.test_type == "upgrade" ) && ( params.test_repo == "main" )) {
                         echo "Skipping as the upgrade and main are not supported together."
                         echo "Exiting the Stage as the inputs are invalid."
                         currentBuild.result = 'UNSTABLE'
                    } else {
                         echo "Continue with the package tests"
                    }                
                }   
                echo "${JENWORKSPACE}"
                installMolecule()
                    sh '''
                        sudo yum install -y epel-release 
                        sudo yum install -y git unzip jq
                        rm -rf package-testing                    
                        git clone https://github.com/panchal-yash/package-testing --branch PXC-package-testing-keyring-script
                    '''
            }
        }
        

                stage("INSTALL") {
                            when {
                                expression{params.test_type == "install" || params.test_type == "install_and_upgrade"}
                            }
                             
                            steps {
                                script{
                                    def param_test_type = "install"   
                                    echo "1. Creating Molecule Instances for running INSTALL PXC tests.. Molecule create step"
                                    runMoleculeAction("create", params.product_to_test, params.node_to_test, "install", params.test_repo, "yes")
                                }
                            }
                            post{
                                always{     
                                    script{
                                        def param_test_type = "install" 
                                        echo "Always INSTALL"
                                        echo "3. Take Backups of the Logs.. PXC INSTALL tests.."
                                        setInventories("install")
                                        def FILEPATH="s3://yash-test-keyring-pxc/pxc80-28-build-install.tar.gz"


                                        def awsCredentials = [
                                            sshUserPrivateKey(
                                                credentialsId: 'MOLECULE_AWS_PRIVATE_KEY',
                                                keyFileVariable: 'MOLECULE_AWS_PRIVATE_KEY',
                                                passphraseVariable: '',
                                                usernameVariable: ''
                                            ),
                                            aws(
                                                accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                                credentialsId: '5d78d9c7-2188-4b16-8e31-4d5782c6ceaa',
                                                secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                                            )
                                        ]

                                        def IN_PXC1_IP = sh(
                                            script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[0] | jq [.private_ip] | jq -r .[]""",
                                            returnStdout: true
                                        ).trim()

                                        def IN_PXC2_IP = sh(
                                            script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[1] | jq [.private_ip] | jq -r .[]""",
                                            returnStdout: true
                                        ).trim()

                                        def IN_PXC3_IP = sh(
                                            script: """cat ${INSTALL_COMMON_INSTANCE_PRIVATE_IP} | jq -r .[2] | jq [.private_ip] | jq -r .[]""",
                                            returnStdout: true
                                        ).trim()

                                        sh """

                                            echo 'PXC1_IP: "${IN_PXC1_IP}"' > "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                                            echo 'PXC2_IP: "${IN_PXC2_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"
                                            echo 'PXC3_IP: "${IN_PXC3_IP}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${param_test_type}/envfile"

                                            sed -i 's/DB1/${IN_PXC1_IP}/g' "${WORKSPACE}/package-testing/scripts/pxc-keyring-test.sh"
                                            sed -i 's/DB2/${IN_PXC2_IP}/g' "${WORKSPACE}/package-testing/scripts/pxc-keyring-test.sh"
                                            sed -i 's/DB3/${IN_PXC3_IP}/g' "${WORKSPACE}/package-testing/scripts/pxc-keyring-test.sh"

                                        """

                                        sh """
                                            echo "Cating the file after sed"
                                            cat ${WORKSPACE}/package-testing/scripts/pxc-keyring-test.sh
                                        """
                                    
                                        withCredentials(awsCredentials) {
                                        
                                            sh """
                                                echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQD6iGdDs3A9vLPFPmJO3pE5TnKBT6grWis3YFcmrMCIj5RsnIdrRRg6Ull0h8ErP+4pyXEGvmwMgEWJ0NBPZL0KynQufLUTFstInEiujLpUsEfj8HpBK25w+/VukT2nX/7UagitH1cZRarAmObtU67cAtOFwBCyM1v2SoeYCKpPyxA2+MVeVVYJnkn3yUTCYDwt77XgeqS4qZ4VyuckiASLAD0/0A3wb81lm2hDB5tZOO50A6ZxdHw9SWGAgEA/i3O+4DPkJ2zd5OntaEIrSHHNT1I8D/BJyFYI9odVdnX+wwfKimqNMnn4Di3lZs2HYdiJ6CP12lp1JrksXH+zqQWvJVwM1rxJ/e638OS9rSfRlzL5TwlEKFTaE48KehpJFjXK0mpG3fbV7NU9K49Gi3gnxNKUEwINlJGLK8d04zlO2gnpkQcrq0HBIN6LAEcDsVRDrNZsERO5I9tw+bBxmmzEeMiU/2NEeLHqqoYPqO3Y27S9xZUvBoI86HcOQwlTwnk= root@yash-ThinkPad-P15-Gen-2i" >> ~/.ssh/authorized_keys
                                                sudo yum install wget -y
                                                curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
                                                unzip -o /tmp/awscliv2.zip -d /tmp 
                                                cd /tmp/aws && sudo ./install
                                                cd ~/
                                                aws s3 cp s3://yash-test-keyring-pxc/pxc80-28-build-install.tar.gz .
                                                ls -la
                                            """

                                            sh """
                                                . virtenv/bin/activate
                                                echo -e "\n\n\n\n" | ssh-keygen -t rsa
                                                export KEY=\$(cat ~/.ssh/id_rsa.pub)
                                                echo "KEY: \"\$KEY\"" > ENVFILE
                                                ansible-playbook ${WORKSPACE}/package-testing/molecule/pxc-keyring-test/pxc-80-setup/playbooks/config-tarballs.yml -i  ${WORKSPACE}/pxc-80-setup/${params.node_to_test}/${param_test_type}/inventory -e @ENVFILE
                                            """

                                            sh """
                                               cd ${WORKSPACE}/package-testing/scripts/
                                               chmod +x pxc-keyring-test.sh
                                               ./pxc-keyring-test.sh
                                            """
                                        
                                        }
                                    }

                                    script{

                                        runlogsbackup(params.product_to_test, "install")
                                        // echo "4. Destroy the Molecule instances for the PXC INSTALL tests.."
                                        // runMoleculeAction("destroy", params.product_to_test, params.node_to_test, "install", params.test_repo, "yes")
                                    }

                                }
                            }
                }
            
        }

        post {

            always {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                    archiveArtifacts artifacts: 'PXC/**/*.tar.gz' , followSymlinks: false
                }
            }

            unstable {
                    slackSend channel: '#dev-server-qa', color: '#DEFF13', message: "[${env.JOB_NAME}]: Failed during the Package testing (Unstable Build) [${env.BUILD_URL}] Parameters: product_to_test: ${params.product_to_test} , node_to_test: ${params.node_to_test} , test_repo: ${params.test_repo}, test_type: ${params.test_type}"
            }

            failure {
                    slackSend channel: '#dev-server-qa', color: '#FF0000', message: "[${env.JOB_NAME}]: Failed during the Package testing (Build Failed) [${env.BUILD_URL}] Parameters: product_to_test: ${params.product_to_test} , node_to_test: ${params.node_to_test} , test_repo: ${params.test_repo}, test_type: ${params.test_type}"
            }


        }
   
    }

