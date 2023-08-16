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
XB_VERSION_MAJOR=8MYV
XB_VERSION_MINOR=0MYV
XB_VERSION_PATCH=33MYV
XB_VERSION_EXTRA=-28MYV
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
                        XB_VERSION_MAJOR = sh(returnStdout: true, script: "grep 'XB_VERSION_MAJOR' ./FILE | cut -d = -f 2 ").trim()
                        XB_VERSION_MINOR = sh(returnStdout: true, script: "grep 'XB_VERSION_MINOR' ./FILE | cut -d = -f 2 ").trim()
                        XB_VERSION_PATCH = sh(returnStdout: true, script: "grep 'XB_VERSION_PATCH' ./FILE | cut -d = -f 2  ").trim()
                        XB_VERSION_EXTRA = sh(returnStdout: true, script: "grep 'XB_VERSION_EXTRA' ./FILE  | cut -d = -f 2 | sed 's/-//g'").trim()
                        echo "The fetched version is ${XB_VERSION_MAJOR}-${XB_VERSION_MINOR}-${XB_VERSION_PATCH}${XB_VERSION_EXTRA}"



                    withCredentials([string(credentialsId: 'PXC_GITHUB_API_TOKEN', variable: 'TOKEN')]) {
                    sh """
                        
                        set -x
                        git clone https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git
                        cd package-testing
                        git checkout pxb-sample-test
                        git config user.name "jenkins-pxc-cd"
                        git config user.email "it+jenkins-pxc-cd@percona.com"
                        OLD_REV=\$(cat VERSIONS | grep PXB80_VER | cut -d '=' -f2- )
                        OLD_VER=\$(cat VERSIONS | grep PXB80PKG_VER | cut -d '=' -f2- )
                        sed -i s/PXB80_VER=\$OLD_REV/PXB80_VER='"'${XB_VERSION_MAJOR}.${XB_VERSION_MINOR}.${XB_VERSION_PATCH}'"'/g VERSIONS
                        sed -i s/PXB80PKG_VER=\$OLD_VER/PXB80PKG_VER='"'${XB_VERSION_EXTRA}'"'/g VERSIONS
                        git diff
                        git add -A
                        git commit -m "Autocommit: add ${XB_VERSION_MAJOR}-${XB_VERSION_MINOR}-${XB_VERSION_PATCH} and ${XB_VERSION_EXTRA} for PXB 80 package testing VERSIONS file."
                        git push 
                    """
                    }



                    }

                }
        }



    }
}
