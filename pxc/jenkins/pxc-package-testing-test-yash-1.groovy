library changelog: false, identifier: 'lib@wip-pxc-package-testing-upgrade-test', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/panchal-yash/jenkins-pipelines.git'
]) _

void installDependencies() {
    sh '''
        export PATH=${PATH}:~/.local/bin
        sudo yum install -y git python3-pip jq
        sudo amazon-linux-extras install ansible2
        python3 -m venv venv
        source venv/bin/activate
        python3 -m pip install setuptools wheel
        python3 -m pip install molecule==2.22 boto boto3 paramiko
    '''
    
    sh '''
        rm -rf package-testing
        git clone https://github.com/panchal-yash/package-testing --branch wip-pxc-package-testing-upgrade-test
    '''

}

void moveSSHKEYS(){
        sh """
                    echo \"Setting up Key path based on the selection\"
                    mkdir -p ${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/
                    mkdir -p ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/
                    cp -a /home/centos/.cache/molecule/${product_to_test}-bootstrap/${params.node_to_test}/ssh_key-us-west-2 ${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/
                    cp -a /home/centos/.cache/molecule/${product_to_test}-common/${params.node_to_test}/ssh_key-us-west-2 ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/
         """
}

def runMoleculeAction(String action, String product_to_test, String scenario, String test_type, String test_repo, String version_check) {
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


            if("${product_to_test}" == "pxc57"){
                def pxc57repo="${params.pxc57_repo}"
            }else{
                echo "Product is not pxc57 so skipping value assignment to it"
            }
            
            echo "asdasdasdas"
            echo "check var ${test_type}"

            sh """
            mkdir -p "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/"
            """

	        if("${test_type}" == "install"){   
                def install_repo="${test_repo}"
                def check_version="${version_check}"
            sh """
                echo 'install_repo: "${install_repo}"' > "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile"
                echo 'check_version: "${check_version}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile"
            """
            }else if("${test_type}" == "upgrade"){
                def install_repo="main"
                def check_version="${version_check}"
                def upgrade_repo="${test_repo}"
            sh """
                echo 'install_repo: "${install_repo}"' > "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile"
                echo 'check_version: "${check_version}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile"
                echo 'upgrade_repo: "${upgrade_repo}"' >> "${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile"
            """
            }else{
                echo "Unknown condition"
            }


            echo "after"
    withCredentials(awsCredentials) {


            if(action == "create"){
                sh"""
                    . virtenv/bin/activate
                    
                    
                    mkdir -p ${WORKSPACE}/install
                    mkdir -p ${WORKSPACE}/upgrade
                    
                    cd package-testing/molecule/pxc
                    export MOLECULE_DEBUG=1
                    
                    cd ${product_to_test}-bootstrap
                    molecule ${action} -s ${scenario}
                    cd -

                    cd ${product_to_test}-common
                    molecule ${action} -s ${scenario}
                    cd -
                """
            }else{
                sh"""
                    . virtenv/bin/activate
                    cd package-testing/molecule/pxc
                    export MOLECULE_DEBUG=1
                    cd ${product_to_test}-bootstrap
                    molecule ${action} -e ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile -s ${scenario}
                    cd -

                    cd ${product_to_test}-common
                    molecule ${action} -e ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile -s ${scenario}
                    cd -
                """
            }
    }
}

void setInventories(){

                    def KEYPATH_BOOTSTRAP
                    def KEYPATH_COMMON
                    def SSH_USER

                    KEYPATH_BOOTSTRAP="/home/centos/.cache/molecule/${product_to_test}-bootstrap/${params.node_to_test}/ssh_key-us-west-2"
                    KEYPATH_COMMON="/home/centos/.cache/molecule/${product_to_test}-common/${params.node_to_test}/ssh_key-us-west-2"


//                    KEYPATH_BOOTSTRAP="${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/ssh_key-us-west-2"
//                    KEYPATH_COMMON="${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/ssh_key-us-west-2"

                    if(("${params.node_to_test}" == "ubuntu-focal")  ||  ("${params.node_to_test}" == "ubuntu-bionic") || ("${params.node_to_test}" == "ubuntu-jammy")){
                        SSH_USER="ubuntu"            
                    }else if(("${params.node_to_test}" == "debian-11") ||  ("${params.node_to_test}" == "debian-10")){
                        SSH_USER="admin"
                    }else if(("${params.node_to_test}" == "ol-8") || ("${params.node_to_test}" == "ol-9") || ("${params.node_to_test}" == "min-amazon-2")){
                        SSH_USER="ec2-user"
                    }else if(("${params.node_to_test}" == "centos-7")){
                        SSH_USER="centos"
                    }else{
                        echo "OS Not yet in list of Keypath setup"
                    }

                    echo "${SSH_USER}"
                    echo "${KEYPATH_BOOTSTRAP}"
                    echo "${KEYPATH_COMMON}"


                if(test_type == "install"){

                    def INSTALL_Bootstrap_Instance = sh(
                        script: """cat ${INSTALL_BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Bootstrap_Instance_Public_IP = sh(
                        script: """cat ${INSTALL_BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC2 = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC2_Public_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC3 = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def INSTALL_Common_Instance_PXC3_Public_IP = sh(
                        script: """cat ${INSTALL_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    sh """
                        mkdir -p "${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/"
                        mkdir -p "${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/"
                        echo \"printing path of bootstrap ${KEYPATH_BOOTSTRAP}"
                        echo \"printing path of common  ${KEYPATH_COMMON}"
                        echo \"printing user ${SSH_USER}"
                        echo "\n ${INSTALL_Bootstrap_Instance} ansible_host=${INSTALL_Bootstrap_Instance_Public_IP}  ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_BOOTSTRAP} ansible_ssh_common_args='-o StrictHostKeyChecking=no' ip_env=${INSTALL_Bootstrap_Instance}" > ${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/inventory            
                        echo "\n ${INSTALL_Common_Instance_PXC2} ansible_host=${INSTALL_Common_Instance_PXC2_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${INSTALL_Common_Instance_PXC2}" > ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/inventory
                        echo "\n ${INSTALL_Common_Instance_PXC3} ansible_host=${INSTALL_Common_Instance_PXC3_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${INSTALL_Common_Instance_PXC3}" >> ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/inventory
                    """

                }else if(params.test_type == "upgrade"){

                    def UPGRADE_Bootstrap_Instance = sh(
                        script: """cat ${UPGRADE_BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def UPGRADE_Bootstrap_Instance_Public_IP = sh(
                        script: """cat ${UPGRADE_BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def UPGRADE_Common_Instance_PXC2 = sh(
                        script: """cat ${UPGRADE_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def UPGRADE_Common_Instance_PXC2_Public_IP = sh(
                        script: """cat ${UPGRADE_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def UPGRADE_Common_Instance_PXC3 = sh(
                        script: """cat ${UPGRADE_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.instance] | jq -r .[]""",
                        returnStdout: true
                    ).trim()

                    def UPGRADE_Common_Instance_PXC3_Public_IP = sh(
                        script: """cat ${UPGRADE_COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.public_ip] | jq -r .[]""",
                        returnStdout: true
                    ).trim()
                    sh """
                        echo \"printing path of bootstrap ${KEYPATH_BOOTSTRAP}"
                        echo \"printing path of common  ${KEYPATH_COMMON}"
                        echo \"printing user ${SSH_USER}"
                        mkdir -p "${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/"
                        mkdir -p "${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/"
                        echo "\n ${UPGRADE_Bootstrap_Instance} ansible_host=${UPGRADE_Bootstrap_Instance_Public_IP}  ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_BOOTSTRAP} ansible_ssh_common_args='-o StrictHostKeyChecking=no' ip_env=${UPGRADE_Bootstrap_Instance}" > ${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/inventory            
                        echo "\n ${UPGRADE_Common_Instance_PXC2} ansible_host=${UPGRADE_Common_Instance_PXC2_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${UPGRADE_Common_Instance_PXC2}" > ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/inventory
                        echo "\n ${UPGRADE_Common_Instance_PXC3} ansible_host=${UPGRADE_Common_Instance_PXC3_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${UPGRADE_Common_Instance_PXC3}" >> ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/inventory
                    """
                    
                }


}

void runlogsbackup(String product_to_test, String test_type) {
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

    withCredentials(awsCredentials) {
        sh """
            . virtenv/bin/activate

            echo "Running the logs backup task for pxc bootstrap node"
            ansible-playbook ${WORKSPACE}/package-testing/molecule/pxc/playbooks/logsbackup.yml -i  ${WORKSPACE}/${product_to_test}-bootstrap/${params.node_to_test}/${test_type}/inventory -e @${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile

            echo "Running the logs backup task for pxc common node"
            ansible-playbook ${WORKSPACE}/package-testing/molecule/pxc/playbooks/logsbackup.yml -i  ${WORKSPACE}/${product_to_test}-common/${params.node_to_test}/${test_type}/inventory -e @${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
        """
    }
    

    
}


def setInstancePrivateIPEnvironment() {
    

            def PXC1_I = sh(
                script: 'jq -r \'.[] | select(.instance | startswith("pxc1")).private_ip\' ${INSTALL_BOOTSTRAP_INSTANCE_PRIVATE_IP}',
                returnStdout: true
            ).trim()
            def PXC2_I = sh(
                script: 'jq -r \'.[] | select(.instance | startswith("pxc2")).private_ip\' ${INSTALL_COMMON_INSTANCE_PRIVATE_IP}',
                returnStdout: true
            ).trim()
            def PXC3_I = sh(
                script: 'jq -r \'.[] | select(.instance | startswith("pxc3")).private_ip\' ${INSTALL_COMMON_INSTANCE_PRIVATE_IP}',
                returnStdout: true
            ).trim()

            
            def PXC1_U = sh(
                script: 'jq -r \'.[] | select(.instance | startswith("pxc1")).private_ip\' ${UPGRADE_BOOTSTRAP_INSTANCE_PRIVATE_IP}',
                returnStdout: true
            ).trim()
            def PXC2_U = sh(
                script: 'jq -r \'.[] | select(.instance | startswith("pxc2")).private_ip\' ${UPGRADE_COMMON_INSTANCE_PRIVATE_IP}',
                returnStdout: true
            ).trim()
            def PXC3_U = sh(
                script: 'jq -r \'.[] | select(.instance | startswith("pxc3")).private_ip\' ${UPGRADE_COMMON_INSTANCE_PRIVATE_IP}',
                returnStdout: true
            ).trim()        
            
}

def setENVS(){

    sh """    
        if [[ ${test_type} == "install" ]];
        then
            echo 'PXC1: "${PXC1_I}"' > ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
            echo 'PXC2: "${PXC2_I}"' >> ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
            echo 'PXC3: "${PXC3_I}"' >> ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
        elif [[ ${test_type} == "upgrade"]]
        then
            echo 'PXC1: "${PXC1_U}"' > ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
            echo 'PXC2: "${PXC2_U}"' >> ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
            echo 'PXC3: "${PXC3_U}"' >> ${WORKSPACE}/${product_to_test}/${params.node_to_test}/${test_type}/envfile
        else
        then
            echo "invalid selection"
        fi
    """

}

pipeline {
    agent {
        label 'min-centos-7-x64'
    }

    options {
        skipDefaultCheckout()
    }

    environment {

        INSTALL_BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/install/bootstrap_instance_private_ip.json"
        INSTALL_COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/install/common_instance_private_ip.json"
        INSTALL_BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/install/bootstrap_instance_public_ip.json"
        INSTALL_COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/install/common_instance_public_ip.json"

        UPGRADE_BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/upgrade/bootstrap_instance_private_ip.json"
        UPGRADE_COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/upgrade/common_instance_private_ip.json"
        UPGRADE_BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/upgrade/bootstrap_instance_public_ip.json"
        UPGRADE_COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/upgrade/common_instance_public_ip.json"

        JENWORKSPACE = "${env.WORKSPACE}"

    }

    parameters {
        choice(
            name: 'product_to_test',
            choices: [
                'pxc80',
                'pxc57'
            ],
            description: 'PXC product_to_test to test'
        )
        choice(
            name: 'node_to_test',
            choices: [
                'ubuntu-jammy',
                'ubuntu-focal',
                'ubuntu-bionic',
                'debian-11',
                'debian-10',
                'centos-7',
                'ol-8',
                'ol-9',
                'min-amazon-2'
            ],
            description: 'Distribution to run test'
        )
        choice(
	        name: 'test_repo',
            choices: [
                'testing',
                'main',
                'experimental'
            ],
            description: 'Repo to install packages from'
        )
        choice(
            name: 'test_type',
            choices: [
                'install',
                'upgrade',
                'install_and_upgrade'
            ],
            description: 'Set test type for testing'
        )      
        choice(
            name: "pxc57_repo",
            choices: ["original","pxc57" ],
            description: "PXC-5.7 packages are located in 2 repos: pxc-57 and original and both should be tested. Choose which repo to use for test."
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
                //installDependencies()
                installMolecule()
                    sh '''
                        sudo yum install -y epel-release 
                        sudo yum install -y git jq
                        rm -rf package-testing                    
                        git clone https://github.com/panchal-yash/package-testing --branch wip-pxc-package-testing-upgrade-test
                    '''
            }
        }
        
        stage("Run parallel Install and UPGRADE"){
            parallel{
                stage("INSTALL") {
                            when {
                                expression{params.test_type == "install" || params.test_type == "install_and_upgrade"}
                            }

                            steps {

                                echo "1. Creating Molecule Instances for running INSTALL PXC tests.. Molecule create step"
                                runMoleculeAction("create", params.product_to_test, params.node_to_test, "install", params.test_repo, "yes")
                                setInstancePrivateIPEnvironment()                                
                                setENVS()                                

                                echo "2. Run Install scripts and tests for PXC INSTALL PXC tests.. Molecule converge step"

                                script{
                                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                                        runMoleculeAction("converge", params.product_to_test, params.node_to_test, "install", params.test_repo, "yes")
                                    }
                                }
                                
                            }
                    
                            post{
                                always{
                                    echo "Always INSTALL"
                                    
                                    echo "3. Take Backups of the Logs.. PXC INSTALL tests.."
                                    setInventories()
                                    runlogsbackup(params.product_to_test, "install")
                                    echo "4. Destroy the Molecule instances for the PXC INSTALL tests.."
                                    runMoleculeAction("destroy", params.product_to_test, params.node_to_test, "install", params.test_repo, "yes")
                                    
                                }
                            }
                }

                stage("UPGRADE") {
                            when {
                                allOf{
                                    expression{params.test_type == "upgrade" || params.test_type == "install_and_upgrade"}
                                    expression{params.test_repo != "main"}                
                                }
                            }
                            
                            steps {
                                echo "UPGRADE STAGE INSIDE"
                                echo "1. Creating Molecule Instances for running PXC UPGRADE tests.. Molecule create step"
                                runMoleculeAction("create", params.product_to_test, params.node_to_test, "upgrade", "main", "no")
                                setInstancePrivateIPEnvironment()                                
                                setENVS()
                                setInventories()
                                echo "2. Run Install scripts and tests for running PXC UPGRADE tests.. Molecule converge step"

                                script{
                                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                                        runMoleculeAction("converge", params.product_to_test, params.node_to_test, "upgrade", "main", "no")
                                    }
                                }

                                echo "3. Run UPGRADE scripts and playbooks for running PXC UPGRADE tests.. Molecule side-effect step"

                                script{
                                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                                        runMoleculeAction("side-effect", params.product_to_test, params.node_to_test, "upgrade", params.test_repo, "yes")
                                    }
                                }

                            }

                            post{
                                always{
                                    echo "POST YPGRADE STAGE"
                                    
                                    echo "4. Take Backups of the Logs.. for PXC UPGRADE tests"
                                    setInventories()
                                    runlogsbackup(params.product_to_test, "upgrade")
                                    echo "5. Destroy the Molecule instances for PXC UPGRADE tests.."
                                    runMoleculeAction("destroy", params.product_to_test, params.node_to_test, "upgrade", params.test_repo, "yes")
                                    
                                }
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
                slackSend channel: '#dev-server-qa', color: '#DEFF13', message: "[${env.JOB_NAME}]: Failed during the Package testing (Unstable Build) [${env.BUILD_URL}] Parameters: product_to_test: ${params.product_to_test} , node_to_test: ${params.node_to_test} , test_repo: ${params.test_repo}"
        }

        failure {
                slackSend channel: '#dev-server-qa', color: '#FF0000', message: "[${env.JOB_NAME}]: Failed during the Package testing (Build Failed) [${env.BUILD_URL}] Parameters: product_to_test: ${params.product_to_test} , node_to_test: ${params.node_to_test} , test_repo: ${params.test_repo}"
        }


    }
   
}
