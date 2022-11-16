library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

List all_nodes = [
                'ubuntu-focal',
                'ubuntu-bionic',
                'debian-11',
                'debian-10',
                'centos-7',
                'ol-8'
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
        job: 'wip-pxc-package-testing',
        parameters: [
            string(name: "product_to_test", value: product_to_test),
            string(name: "install_repo", value: params.install_repo),
            string(name: "node_to_test", value: node_to_test)
        ],
        propagate: true,
        wait: true
    )
}

pipeline {
    agent none

    parameters {
        choice(
            name: "product_to_test",
            choices: ["pxc80", "pxc57"],
            description: "Product for which the packages will be tested"
        )

        choice(
            name: "install_repo",
            choices: ["testing", "main", "experimental"],
            description: "Repo to use in install test"
        )

        choice(
            name: "node_to_test",
            choices: ["all"] + all_nodes,
            description: "Node in which to test the product"
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
                stage("Debian-10") {
                    when {
                        expression {
                            nodes_to_test.contains("debian-10")
                        }
                    }

                    steps {
                        runNodeBuild("debian-10")
                    }
                }

                stage("Debian-11") {
                    when {
                        expression {
                            nodes_to_test.contains("debian-11")
                        }
                    }

                    steps {
                        runNodeBuild("debian-11")
                    }
                }

                stage("Centos 7") {
                    when {
                        expression {
                            nodes_to_test.contains("centos-7")
                        }
                    }

                    steps {
                        runNodeBuild("centos-7")
                    }
                }

                stage("ol-8") {
                    when {
                        expression {
                            nodes_to_test.contains("ol-8")
                        }
                    }

                    steps {
                        runNodeBuild("ol-8")
                    }
                }

                stage("ubuntu-bionic") {
                    when {
                        expression {
                            nodes_to_test.contains("ubuntu-bionic")
                        }
                    }

                    steps {
                        runNodeBuild("ubuntu-bionic")
                    }
                }

                stage("ubuntu-focal") {
                    when {
                        expression {
                            nodes_to_test.contains("ubuntu-focal")
                        }
                    }

                    steps {
                        runNodeBuild("ubuntu-focal")
                    }
                }

            }
        }
    }
}
