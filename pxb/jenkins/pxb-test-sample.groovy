library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

pipeline {
    agent { label 'docker' }

    stages {
        stage("Prepare") {
            steps {
                script {
                    sh """
cat <<EOF > FILE
XB_VERSION_MAJOR=8
XB_VERSION_MINOR=0
XB_VERSION_PATCH=33
XB_VERSION_EXTRA=-28
REVISION=b3a3c3dd
BRANCH_NAME=release-8.0.33-28
PRODUCT=Percona-XtraBackup-8.0
PRODUCT_FULL=Percona-XtraBackup-8.0.33-28
PRODUCT_UL_DIR=Percona-XtraBackup-8.0
EOF
                    """
                }
            }
        }

        stage("Check stuff"){
                steps {

                    script {
                        sh "cat FILE"
                        XB_VERSION_MAJOR = sh(returnStdout: true, script: "source ./FILE && echo \$XB_VERSION_MAJOR").trim()
                        XB_VERSION_MINOR = sh(returnStdout: true, script: "source ./FILE && echo \$XB_VERSION_MINOR").trim()
                        XB_VERSION_PATCH = sh(returnStdout: true, script: "source ./FILE && echo \$XB_VERSION_PATCH").trim()
                        XB_VERSION_EXTRA = sh(returnStdout: true, script: "source ./FILE && echo \$XB_VERSION_EXTRA").trim()
                        echo "The fetched version is ${XB_VERSION_MAJOR}-${XB_VERSION_MINOR}-${XB_VERSION_PATCH}${XB_VERSION_EXTRA}"
                    }

                }
        }



    }
}
