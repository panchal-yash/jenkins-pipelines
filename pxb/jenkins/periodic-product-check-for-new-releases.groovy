library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _


void runNodeBuild(String node_to_test) {
    build(
        job: 'periodic-product-check-for-new-releases-build',
        propagate: true,
        parameters: [
        string(name: "node_to_test", value: node_to_test),
        ],
        wait: true
    )
}

List all_nodes = [
    "min-buster-x64",
    "min-bullseye-x64",
    "min-centos-7-x64",
    "min-centos-8-x64",    
    "min-ol-8-x64",
    "min-bionic-x64",
    "min-focal-x64",
  //  "min-amazon-2-x64",
]


pipeline {
    agent none
    triggers {
        cron('0 0 * * 0')
    }


    stages {
        stage("Prepare") {
            steps {
                script {
                    currentBuild.displayName = "#${BUILD_NUMBER}"
                    currentBuild.description = "Install percona-release for checking.. node: ${params.node_to_test}"
                }
            }
        }

        stage("Run parallel") {
            parallel {
                stage("Debian Bullseye") {
                    steps {
                        runNodeBuild("min-bullseye-x64")
                    }
                }

                stage("Debian Buster") {
                    steps {
                        runNodeBuild("min-buster-x64")
                    }
                }

                stage("Centos 7") {
                    steps {
                        runNodeBuild("min-centos-7-x64")
                    }
                }

                stage("Centos 8") {
                    steps {
                        runNodeBuild("min-centos-8-x64")
                    }
                }

                stage("Oracle Linux 8") {
                    steps {
                        runNodeBuild("min-ol-8-x64")
                    }
                }

                stage("Ubuntu Bionic") {
                    steps {
                        runNodeBuild("min-bionic-x64")
                    }
                }

                stage("Ubuntu Focal") {
                     steps {
                        runNodeBuild("min-focal-x64")
                    }
                }
/*
                stage("Amazon Linux") {
                     steps {
                        runNodeBuild("min-amazon-2-x64")
                    }
                }
*/                
            }
        }
    }
}
