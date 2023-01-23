library changelog: false, identifier: 'lib@wip-pxc-package-testing-upgrade-test', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/panchal-yash/jenkins-pipelines.git'
]) _

List all_nodes = [
                'ubuntu-jammy',
                'ubuntu-focal'
]

product_to_test = params.product_to_test

List nodes_to_test = []
if (params.node_to_test == "all") {
    nodes_to_test = all_nodes
} else {
    nodes_to_test = [params.node_to_test]
}

void runNodeBuild(String node_to_test) {
    build(
        job: 'wip-pxc-package-testing-test-1',
        parameters: [
            string(name: "product_to_test", value: product_to_test),
            string(name: "node_to_test", value: node_to_test),
            string(name: "test_repo", value: params.test_repo),
            string(name: "test_type", value: test_type),
            string(name: "pxc57_repo", value: pxc57_repo)            
        ],
        propagate: true,
        wait: true
    )
}


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
    environment {

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
            name: "node_to_test",
            choices: ["all"] + all_nodes,
            description: "Node in which to test the product"
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
        stage("Prepare") {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}-${product_to_test}-${params.install_repo}-${params.node_to_test}"
                    currentBuild.description = "action: ${params.action_to_test} node: ${params.node_to_test}"
                }
            }
        }


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

        stage('Test') {
          steps {
                script {
                    moleculepxcParallelTest(all_nodes, test_type)
                }
            }
        }        
    }

    post {
        always {
          script {
              moleculepxcParallelDestroy(all_nodes, params.product_to_test)
              catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                archiveArtifacts artifacts: 'PXC/**/*.tar.gz' , followSymlinks: false
             }
         }
      }
   }


}
