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


setup_debian = { ->
    sh '''
        sudo apt-get update -y
        sudo apt install curl unzip -y
        curl -O https://repo.percona.com/apt/percona-release_latest.generic_all.deb
        sudo apt install gnupg2 lsb-release ./percona-release_latest.generic_all.deb -y
        sudo apt update -y
        
        sudo percona-release show
    '''
}

setup_rhel = { ->
    sh '''


        if [ -f "/usr/local/bin/aws" ]; then
        
            echo "AWS CLI already exists"

        else

            sudo yum update -y
            sudo yum reinstall -y https://repo.percona.com/yum/percona-release-latest.noarch.rpm || sudo yum install -y https://repo.percona.com/yum/percona-release-latest.noarch.rpm
            sudo percona-release show
            sudo yum install unzip -y
            echo "Installing aws cli"
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            unzip awscliv2.zip
            sudo ./aws/install

        fi




    '''
}


node_setups = [
    "min-bullseye-x64": setup_debian,
    "min-buster-x64": setup_debian,
    "min-centos-7-x64": setup_rhel,
    "min-centos-8-x64": setup_rhel,
    "min-ol-8-x64": setup_rhel,
    "min-bionic-x64": setup_debian,
    "min-focal-x64": setup_debian,
  //  "min-amazon-2-x64": setup_rhel,
]

void setup_package_tests() {
    node_setups[params.node_to_test]()
}

void checkrhelpackage(String packagecode , String packagename , String reponame, String platform){
sh """

set +x 

if [ -f "/etc/yum.repos.d/percona-prel-release.repo" ]; then
    sudo rm -f /etc/yum.repos.d/percona-prel-release.repo
else 
    echo "/etc/yum.repos.d/percona-prel-release.repo does not exist."
fi

sudo percona-release show

sudo percona-release enable-only ${packagecode} ${reponame}

yum --showduplicates list | grep -i ${packagename} | awk '{ print\$2}' > ${packagecode}-${platform}-${reponame}

echo "-----------${packagecode}-${platform}-releases-----------"

cat ${packagecode}-${platform}-${reponame}

#echo "asdasdas" >> ${packagecode}-${platform}-${reponame}

cat ${packagecode}-${platform}-${reponame} | wc -l > ${packagecode}-${platform}-${reponame}-nos 

echo "-----------${packagecode}-${platform}-${reponame}-releases-count-----------"

cat ${packagecode}-${platform}-${reponame}-nos



"""

}

void checkdebpackage(String packagecode , String packagename , String reponame, String platform){
sh """

set +x 

if [ -f "/etc/apt/sources.list.d/percona-prel-release.list" ]; then
    sudo rm -f /etc/apt/sources.list.d/percona-prel-release.list
else 
    echo "/etc/apt/sources.list.d/percona-prel-release.list does not exist."
fi

sudo percona-release show

sudo percona-release enable-only ${packagecode} ${reponame}

sudo apt-get update

sudo apt -a list | grep -i ${packagename} | awk '{ print\$2}' > ${packagecode}-${platform}-${reponame}

echo "-----------${packagecode}-${platform}-${reponame}-releases-----------"

cat ${packagecode}-${platform}-${reponame}

#echo "asdasdas" >> ${packagecode}-${platform}-${reponame}

cat ${packagecode}-${platform}-${reponame} | wc -l > ${packagecode}-${platform}-${reponame}-nos 

echo "-----------${packagecode}-${platform}-${reponame}-releases-count-----------"

cat ${packagecode}-${platform}-${reponame}-nos


"""

}

void popcheckandpush(String packagecode , String packagename , String reponame, String platform){

    echo "1"

    checkArtifactFile("${packagecode}-${platform}-${reponame}")

    if( "${exists}" > 1 ){
        echo "Here"
        popArtifactFile("${packagecode}-${platform}-${reponame}")
        sh "mv ${packagecode}-${platform}-${reponame} ${packagecode}-${platform}-${reponame}-previous"

        
        if( "${platform}" == "centos-7" || "${platform}" == "centos-8" || "${platform}" == "ol-8" || "${platform}" == "al-2" ){
            echo "RHEL Selected"
            checkrhelpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")

        }
        else if("${platform}" == "debian-10" || "${platform}" == "debian-11" || "${platform}" == "focal" || "${platform}" == "bionic"){
            echo "Debian Selected"   
            checkdebpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")
        }

        else {
            echo "Another OS"
        }


        if ( sh(script: "diff ${packagecode}-${platform}-${reponame} ${packagecode}-${platform}-${reponame}-previous > ${packagecode}-${platform}-${reponame}-diff 2>&1", returnStatus:true ) ){

            def diff_out = sh(script: "cat ${packagecode}-${platform}-${reponame}-diff", returnStdout: true)
            pushArtifactFile("${packagecode}-${platform}-${reponame}")
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
        else if("${platform}" == "debian-10" || "${platform}" == "debian-11" || "${platform}" == "focal" || "${platform}" == "bionic" ){
            echo "Debian Selected"   
            checkdebpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")
        }

        else {
            echo "Another OS"
        }

        //checkrhelpackage("${packagecode}","${packagename}" , "${reponame}", "${platform}")
        pushArtifactFile("${packagecode}-${platform}-${reponame}")
        slackSend channel: '#new-product-release-detection-jenkins', color: '#FF0000', message: "Pushing the artifact for the ${packagecode}-${platform}-${reponame} package"

    }   
 
}


List all_nodes = node_setups.keySet().collect()


pipeline {
    agent { label params.node_to_test }

    options {
        skipDefaultCheckout()
    }
    
    parameters{
        choice(
            name: "node_to_test",
            choices: all_nodes,
            description: "Node in which to test the script"
        )
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
                        currentBuild.displayName = "#${BUILD_NUMBER}-${params.node_to_test}"
                        currentBuild.description = "action: install and check the percona-release"
                    }
                }
            }

            stage("Setup the Server"){
                steps {
                    setup_package_tests()
                }
            }

            stage("check os") {
                steps {
                    echo "cat /etc/os-release"
                }
            }

            stage("Show existing percona-release enabled packages") {
                steps {
                    sh "sudo percona-release show"
                }
            }

            stage("Disable existing percona-released enabled repos") {
                steps {
                    sh "sudo percona-release disable all"
                }
            }
            
            stage("check for the packages again") {
                steps {
                    sh "sudo percona-release show"
                }
            }

            stage("OS based checks") {
                steps{
                    script {
                        if (node_to_test.contains("min-centos-7-x64")) {

                            popcheckandpush("pxb-24","percona-xtrabackup-24.x86_64" , "testing", "centos-7")
                            popcheckandpush("pxb-80","percona-xtrabackup-80.x86_64" , "testing", "centos-7")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "centos-7")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "centos-7")
                            popcheckandpush("ps-56","percona-server-server" , "testing", "centos-7")
                            popcheckandpush("pxc-80","percona-xtradb-cluster.x86" , "testing", "centos-7")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-57.x86" , "testing", "centos-7")

                        }
                        else if (node_to_test.contains("min-centos-8-x64")){
                            
                            popcheckandpush("pxb-24","percona-xtrabackup-24.x86_64" , "testing", "centos-8")
                            popcheckandpush("pxb-80","percona-xtrabackup-80.x86_64" , "testing", "centos-8")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "centos-8")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "centos-8")
                            popcheckandpush("ps-56","percona-server-server" , "testing", "centos-8")
                            popcheckandpush("pxc-80","percona-xtradb-cluster-full" , "testing", "centos-8")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-full" , "testing", "centos-8")

                        }
                        else if (node_to_test.contains("min-ol-8-x64")){
                     
                            popcheckandpush("pxb-24","percona-xtrabackup-24.x86_64" , "testing", "ol-8")
                            popcheckandpush("pxb-80","percona-xtrabackup-80.x86_64" , "testing", "ol-8")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "ol-8")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "ol-8")
                            popcheckandpush("ps-56","percona-server-server" , "testing", "ol-8")
                            popcheckandpush("pxc-80","percona-xtradb-cluster-full" , "testing", "ol-8")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-full" , "testing", "ol-8")

                        }

                        else if (node_to_test.contains("min-buster-x64")){
                        
                            popcheckandpush("pxb-24","percona-xtrabackup-24" , "testing", "debian-10")
                            popcheckandpush("pxb-80","percona-xtrabackup-80" , "testing", "debian-10")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "debian-10")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "debian-10")
                            popcheckandpush("ps-56","percona-server-server" , "testing", "debian-10")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-57" , "testing", "debian-10")
                            popcheckandpush("pxc-80","percona-xtradb-cluster-server/" , "testing", "debian-10")

                        }

                        else if (node_to_test.contains("min-bullseye-x64")){
                        
                            popcheckandpush("pxb-24","percona-xtrabackup-24" , "testing", "debian-11")
                            popcheckandpush("pxb-80","percona-xtrabackup-80" , "testing", "debian-11")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "debian-11")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "debian-11")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-57" , "testing", "debian-11")
                            popcheckandpush("pxc-80","percona-xtradb-cluster-server/" , "testing", "debian-11")

                        }
                        
                        else if (node_to_test.contains("min-focal-x64")){
                        
                            popcheckandpush("pxb-24","percona-xtrabackup-24" , "testing", "focal")
                            popcheckandpush("pxb-80","percona-xtrabackup-80" , "testing", "focal")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "focal")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "focal")
                            popcheckandpush("ps-56","percona-server-server" , "testing", "focal")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-57" , "testing", "focal")
                            popcheckandpush("pxc-80","percona-xtradb-cluster-server/" , "testing", "focal")

                        }

                        else if (node_to_test.contains("min-bionic-x64")){
                        
                            popcheckandpush("pxb-24","percona-xtrabackup-24" , "testing", "bionic")
                            popcheckandpush("pxb-80","percona-xtrabackup-80" , "testing", "bionic")
                            popcheckandpush("ps-80","percona-server-server" , "testing", "bionic")
                            popcheckandpush("ps-57","percona-server-server" , "testing", "bionic")
                            popcheckandpush("ps-56","percona-server-server" , "testing", "bionic")
                            popcheckandpush("pxc-57","percona-xtradb-cluster-57" , "testing", "bionic")
                            popcheckandpush("pxc-80","percona-xtradb-cluster-server/" , "testing", "bionic")

                        }                        


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
