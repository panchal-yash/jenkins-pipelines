 library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _
def REVISION="evision"
def PS_RELEASE="samplerelease"        
pipeline {
    agent {
        label 'micro-amazon'
    }
    options {
        skipDefaultCheckout()
    }

    stages {


        stage("Cleanup Workspace") {
            steps {                
                sh "sudo rm -rf ${WORKSPACE}/*"
            }
        }

        stage("Checks") {
                steps {                
                sh "echo 'hello'"      
                }
        }

    }
            
    post {
        success {
        
             script{


                    withCredentials([string(credentialsId: 'PXC_GITHUB_API_TOKEN', variable: 'TOKEN')]) {

                    sh """
 
                    git clone https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git
                    cd package-testing
                    git config user.name "jenkins-pxc-cd"
                    git config user.email "it+jenkins-pxc-cd@percona.com"
                    OLD_REV=\$(cat VERSIONS | grep PS80_REV | cut -d '=' -f2- | sed 's/"//g')
                    OLD_VER=\$(cat VERSIONS | grep PS80_VER | cut -d '=' -f2- | sed 's/"//g')
                    sed -i "s/PS80_REV=\"\$OLD_REV\"/PS80_REV=\"${REVISION}\"/g" VERSIONS
                    sed -i "s/PS80_VER=\"\$OLD_VER\"/PS80_VER=\"${PS_RELEASE}\"/g" VERSIONS
                    git diff
                    git add -A
                    git commit -m "Autocommit: add ${REVISION} and ${PS_RELEASE} for ps80 package testing VERSIONS file."
                    git push 

                    """

                    }
             }
        }
        

    }
}
