library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

product_to_test = params.product_to_test

List nodes_to_test = []
if (params.node_to_test == "all") {
    nodes_to_test = all_nodes
} else {
    nodes_to_test = [params.node_to_test]
}


product_action_playbooks = [
    ps80: [
        install: "ps_80.yml",
        upgrade: "ps_80_upgrade.yml",
        "maj-upgrade-to": "ps_80_major_upgrade_to.yml",
        kmip: "ps_80_kmip.yml",
    ],
    client_test: [
        install: "client_test.yml",
        upgrade: "client_test_upgrade.yml",
    ]
]

Map product_actions = product_action_playbooks.collectEntries { key, value ->
    [key, value.keySet().collect()]
}



pipeline {
    agent none

    environment {
        product_to_test = 'ps80'
        install_repo = 'testing'
        action_to_test = 'install'
        check_warnings = 'no'
        install_mysql_shell = 'no'
    }

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
                    def arrayA = [  "min-buster-x64",
                                    "min-bullseye-x64",
                                    "min-bookworm-x64",
                                    "min-centos-7-x64",
                                    "min-ol-8-x64",
                                    "min-bionic-x64",
                                    "min-focal-x64",
                                    "min-amazon-2-x64",
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

                                        def playbook = product_action_playbooks[env.product_to_test][action_to_test]
                                        def playbook_path = "package-testing/playbooks/${playbook}"



                                        sh '''
                                            git clone --depth 1 https://github.com/Percona-QA/package-testing
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

                                        if (currentBuild.result == 'FAILURE') {
                                            echo "Build failed on ${nodeName}"
                                            // Additional steps for failure case
                                        } else if (currentBuild.result == 'UNSTABLE') {
                                            echo "Build unstable on ${nodeName}"
                                            // Additional steps for unstable case
                                        } else {
                                            echo "Build succeeded on ${nodeName}"
                                            // Additional steps for success case
                                        }
                                    }
                                    
                                }
                        }
                    }
                    parallel stepsForParallel
                }
            }
        }


    }

}
