library changelog: false, identifier: 'lib@master', retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/Percona-Lab/jenkins-pipelines.git'
]) _



void pushArtifactFile(String FILE_NAME) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'AMI/OVF', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """
            S3_PATH=s3://product-release-check
            aws s3 ls \$S3_PATH/${FILE_NAME} || :
            aws s3 cp --quiet ${FILE_NAME} \$S3_PATH/${FILE_NAME} || :
        """
    }
}

void popArtifactFile(String FILE_NAME) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'AMI/OVF', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """
            S3_PATH=s3://product-release-check
            aws s3 cp --quiet \$S3_PATH/${FILE_NAME} ${FILE_NAME} || :
        """
    }
}

setup_debian = { ->
    sh '''
        sudo apt-get update -y
        sudo apt install curl -y
        curl -O https://repo.percona.com/apt/percona-release_latest.generic_all.deb
        sudo apt install gnupg2 lsb-release ./percona-release_latest.generic_all.deb -y
        sudo apt update -y

        sudo percona-release show
    '''
}

setup_rhel = { ->
    sh '''
        sudo yum update -y
        sudo yum reinstall -y https://repo.percona.com/yum/percona-release-latest.noarch.rpm || sudo yum install -y https://repo.percona.com/yum/percona-release-latest.noarch.rpm
        sudo percona-release show
    '''
}


node_setups = [
    "min-bullseye-x64": setup_debian,
    "min-buster-x64": setup_debian,
    "min-centos-7-x64": setup_rhel,
    "min-ol-8-x64": setup_rhel,
    "min-bionic-x64": setup_debian,
    "min-focal-x64": setup_debian,
    "min-amazon-2-x64": setup_rhel,
]

void setup_package_tests() {
    node_setups[params.node_to_test]()
}

void bullseye() {
 

}

void buster() {


}

void centos7() {

sh """ 

if [ -f "/etc/yum.repos.d/percona-prel-release.repo" ]; then
    sudo rm -f /etc/yum.repos.d/percona-prel-release.repo
else 
    echo "/etc/yum.repos.d/percona-prel-release.repo does not exist."
fi

sudo percona-release show

sudo yum --showduplicates list | grep percona

sudo percona-release enable pxb-80 testing

yum --showduplicates list | grep percona-xtrabackup-80.x86_64 | awk '{ print\$2}' > pxb-80-centos-7

echo "-----------PXB-80-CENTOS-7-releases-----------"

cat pxb-80-centos-7

echo "asdaasd" >> pxb-80-centos-7

cat pxb-80-centos-7 | wc -l > pxb-80-centos-7-nos 

echo "-----------PXB-80-CENTOS-7-releases-count-----------"

cat pxb-80-centos-7-nos

"""

}


void fetchartifact( String component){
 copyArtifacts filter: "${component}*", projectName: 'periodic-product-check-for-new-releases-build', 
 selector: lastSuccessful(true), 
 target: 'previous' 
}

void diffchecker(String filename , String filepath1 , String filepath2){

sh """

set +e
diff ${filepath1} ${filepath2} > ${filename}-diff 2>&1 || echo "Found difference"

"""

}



void ol8() {


}

void bionic() {


}

void focal() {


}

void amazon() {


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
                            fetchartifact("pxb-80-centos-7")
                            centos7()
                            pushArtifactFile("pxb-80-centos-7")
                            diffchecker("pxb-80-centos-7", "pxb-80-centos-7", "previous/pxb-80-centos-7")
                            sh "cat pxb-80-centos-7-diff"
                        } 
                        else if (node_to_test.contains("min-bullseye-x64")){
                            bullseye()
                        }
                        else if (node_to_test.contains("min-buster-x64")){
                            buster()
                        }
                    }
                }
                
            }

    }
}
