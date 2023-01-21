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

pipeline {
    agent {
        label 'micro-amazon'
    }

    options {
        skipDefaultCheckout()
    }

    environment {

        BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/bootstrap_instance_private_ip.json"
        COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/common_instance_private_ip.json"

        BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/bootstrap_instance_public_ip.json"
        COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/common_instance_public_ip.json"

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
                installDependencies()
            }
        }

        stage("INSTALL") {

            when {
                expression{params.test_type == "install" || params.test_type == "install_and_upgrade"}
            }

            steps {
                
                moleculepxcRunMoleculeAction1("create", params.product_to_test, params.node_to_test, "install", params.test_repo, "yes")

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

                moleculepxcPackageTestsUPGRADE(params.node_to_test)
                
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
