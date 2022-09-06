library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _



void pushArtifactFile(String FILE_NAME) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: '24e68886-c552-4033-8503-ed85bbaa31f3', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """
            
            S3_PATH=s3://product-release-check
            aws s3 ls \$S3_PATH/${FILE_NAME} || :
            aws s3 cp --quiet ${FILE_NAME} \$S3_PATH/${FILE_NAME} || :

        """
    }
}

void popArtifactFile(String FILE_NAME) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: '24e68886-c552-4033-8503-ed85bbaa31f3', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """

            S3_PATH=s3://product-release-check
            aws s3 cp --quiet \$S3_PATH/${FILE_NAME} ${FILE_NAME} || :
        
        """
    }
}

void checkArtifactFile(String FILE_NAME) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: '24e68886-c552-4033-8503-ed85bbaa31f3', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            S3_PATH="s3://product-release-check"
            exists=sh(script: "aws s3 ls ${S3_PATH}/${FILE_NAME} | wc -l ", returnStdout: true)
    }
}

check = { ->
    sh '''        
            echo "HELLO THERE"
    '''
}


setup_debian = { ->
    sh '''
        sudo apt-get update -y
        sudo apt install curl unzip lftp -y 
    '''
}

setup_rhel = { ->
    sh '''


        if [ -f "/usr/local/bin/aws" ]; then
        
            echo "AWS CLI already exists"

        else

            sudo yum update -y
            sudo yum install unzip lftp -y
            echo "Installing aws cli"
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            unzip awscliv2.zip
            sudo ./aws/install

        fi
    '''
}



void checkrhelpackage(String packagecode , String packagename , String reponame, String platform){
sh """





"""

}

void checkdebpackage(String packagecode , String packagename , String reponame, String platform){
sh """

"""
}

void popcheckandpush(String packagecode , String packagename , String reponame, String platform){

    echo "1"

    checkArtifactFile("${packagecode}-${platform}")

    if( "${exists}" > 1 ){
        echo "Here"
        popArtifactFile("${packagecode}-${platform}")
        sh "mv ${packagecode}-${platform} ${packagecode}-${platform}-previous"

        
        if( "${platform}" == "centos-7" || "${platform}" == "centos-8" || "${platform}" == "ol-8" || "${platform}" == "al-2" ){
            echo "RHEL Selected"
            checkrhelpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")

        }
        else if("${platform}" == "debian-10" || "${platform}" == "debian-11"){
            echo "Debian Selected"   
            checkdebpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")
        }

        else {
            echo "Another OS"
        }


        if ( sh(script: "diff ${packagecode}-${platform} ${packagecode}-${platform}-previous > ${packagecode}-${platform}-diff 2>&1", returnStatus:true ) ){

            def diff_out = sh(script: "cat ${packagecode}-${platform}-diff", returnStdout: true)
            pushArtifactFile("${packagecode}-${platform}")
            slackSend channel: '#new-product-release-detection-jenkins', color: '#FF0000', message: "Found difference in releases: ${diff_out}. we need to run jenkins job ${BUILD_URL}"

        }
        else {

            echo "There is no difference"

        }

    }
    else{
        echo "there"
        if( "${platform}" == "centos-7" || "${platform}" == "centos-8" || "${platform}" == "ol-8" || "${platform}" == "al-2" ){
            echo "RHEL Selected"
            checkrhelpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")

        }
        else if("${platform}" == "debian-10" || "${platform}" == "debian-11"){
            echo "Debian Selected"   
            checkdebpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")
        }

        else {
            echo "Another OS"
        }

        //checkrhelpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")
        pushArtifactFile("${packagecode}-${platform}")
        slackSend channel: '#new-product-release-detection-jenkins', color: '#FF0000', message: "Pushing the artifact for the ${packagecode}-${platform} package"

    }   
 
}


pipeline {
    agent { 
         label 'min-bullseye-x64'
      }

    options {
        skipDefaultCheckout()
    }
    
    stages {
            stage("clean workspace"){
                steps{

                    cleanWs()

                }
            }

            stage("Prepare") {
                steps {
                    script {
                        currentBuild.displayName = "#${BUILD_NUMBER}"
                        currentBuild.description = "action: install and check the percona-release"
                    }
                }
            }

            // stage("Setup the Server"){
                // steps {
                    // setup_package_tests()
                // }
            // }

            stage("check os") {
                steps {
                    echo "cat /etc/os-release"
                }
            }

            stage("OS based checks") {
                steps{
                        script{
                          sh """
                                wget https://raw.githubusercontent.com/panchal-yash/percona-basic-scripts/main/check.sh
                                chmod +x check.sh
                                ./check.sh
                             """

                        }
                    }
            }



    }

    post {
        failure {
            slackSend channel: '#new-product-release-detection-jenkins', color: '#FF0000', message: "Build Failed due to errors ${BUILD_URL}"
        }
    }

}














