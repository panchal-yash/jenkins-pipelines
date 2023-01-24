library changelog: false, identifier: 'lib@wip-pxc-package-testing-upgrade-test', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/panchal-yash/jenkins-pipelines.git'
]) _

List all_nodes = [
                'ubuntu-jammy',
                'ubuntu-focal',
                'ubuntu-bionic',
                'debian-11',
                'debian-10',
                'centos-7',
                'ol-8',
                'ol-9',
                'min-amazon-2'
]

product_to_test = params.product_to_test

List nodes_to_test = []
if (params.node_to_test == "all") {
    nodes_to_test = all_nodes
} else {
    nodes_to_test = [params.node_to_test]
}

void runNodeBuild(String node_to_test, String test_type_value) {
    build(
        job: 'wip-pxc-package-testing-test-1',
        parameters: [
            string(name: "product_to_test", value: product_to_test),
            string(name: "node_to_test", value: node_to_test),
            string(name: "test_repo", value: params.test_repo),
            string(name: "test_type", value: test_type_value),
            string(name: "pxc57_repo", value: pxc57_repo)            
        ],
        propagate: true,
        wait: true
    )
}

pipeline {
    agent {
        label 'docker'
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

        stage("Run parallel") {
            parallel {
                stage("Debian-10 INSTALL") {
                    when {
                        expression {
                            allOf{
                                nodes_to_test.contains("debian-10")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                            }
                        }
                    }

                    steps {
                        runNodeBuild("debian-10","install")
                    }
                }

                stage("Debian-11 INSTALL") {
                    when {
                        expression {
                            allOf{
                                nodes_to_test.contains("debian-11")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                            }
                        }
                    }

                    steps {
                        runNodeBuild("debian-11","install")
                    }
                }

                stage("Centos 7 INSTALL") {
                    when {
                        expression {
                            allOf{                            
                                nodes_to_test.contains("centos-7")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                            }
                        }
                    }

                    steps {
                        runNodeBuild("centos-7","install")
                    }
                }

                stage("ol-8 INSTALL") {
                    when {
                        expression {
                            allOf{
                                nodes_to_test.contains("ol-8")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                        }
                    }

                    steps {
                        runNodeBuild("ol-8","install")
                    }
                }

                stage("ol-9 INSTALL") {
                    when {
                        expression {
                            allOf{
                                nodes_to_test.contains("ol-9")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }                            
                            }
                        }
                    }

                    steps {
                        runNodeBuild("ol-9","install")
                    }
                }


                stage("ubuntu-jammy INSTALL") {
                    when {
                        expression {
                            allOf{                            
                                nodes_to_test.contains("ubuntu-jammy")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                            }
                        }
                    }

                    steps {
                        runNodeBuild("ubuntu-jammy","install")
                    }
                }

                stage("ubuntu-bionic INSTALL") {
                    when {
                        expression {
                            allOf{
                                nodes_to_test.contains("ubuntu-bionic")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }                            
                            }
                        }
                    }

                    steps {
                        runNodeBuild("ubuntu-bionic","install")
                    }
                }

                stage("ubuntu-focal INSTALL") {
                    when {
                        expression {
                            allOf{
                                nodes_to_test.contains("ubuntu-focal")
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                            }
                        }
                    }

                    steps {
                        runNodeBuild("ubuntu-focal","install")
                    }
                }

	            stage("min-amazon-2 INSTALL") {	
                    when {	
                        expression {	
                            allOf{
                                nodes_to_test.contains("min-amazon-2")	
                                anyOf{
                                    nodes_to_test.contains("install")
                                    nodes_to_test.contains("install_and_upgrade")
                                }
                            }
                        }	
                    }	
                    steps {	
                        runNodeBuild("min-amazon-2","install")	
                    }	
                }

            }
        }
    }
}
