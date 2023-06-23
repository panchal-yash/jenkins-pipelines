library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _

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

            REVISION = "SAMPLE_REVISION" 
            PS_RELEASE="SAMPLE_VALUE"
                
             script{


                    withCredentials([string(credentialsId: 'PXC_GITHUB_API_TOKEN', variable: 'TOKEN')]) {

                    sh """
 
                    git clone https://jenkins-pxc-cd:$TOKEN@github.com/Percona-QA/package-testing.git
                    cd package-testing
                    git config user.name "jenkins-pxc-cd"
                    git config user.email "it+jenkins-pxc-cd@percona.com"
                    cat .git/config
                    OLD_REV=\$(cat VERSIONS | grep PS80_REV | cut -d '=' -f2- | sed 's/"//g')
                    OLD_VER=\$(cat VERSIONS | grep PS80_VER | cut -d '=' -f2- | sed 's/"//g')
                    sed -i "s/\$OLD_REV/${REVISION}/g" VERSIONS
                    sed -i "s/\$OLD_VER/${PS_RELEASE}/g" VERSIONS
                    git diff
                    git add -A
                    git commit -m "add ${REVISION} and ${PS_RELEASE} for ps80 package testing VERSIONS file."
                    git push -f 

                    """

                    }
             }

            }
        }
        
    }




    }
