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



node_setups = [
    "min-buster-x64": setup_debian,
//   "min-centos-7-x64": setup_rhel,
]

void setup_package_tests() {
    node_setups[params.node_to_test]()
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

            stage("OS based checks") {
                steps{
                        script{
                          sh '''
                            #!/bin/bash

                            set -x

                            ##--------------------------------------DEBIAN------------------------------------------------

                            PXC_80=("pxc-80")
                            PXC_80_repo_version=("p") # 2017 and 2018 dirs had 404 error
                            PXC_80_component_name=("percona-xtradb-cluster-full")
                            PXC_80_component_repository=("testing" "experimental" "main" "laboratory")
                            PXC_80_component_path=("percona-xtradb-cluster")

                            PXC_56=("pxc-56")
                            PXC_56_repo_version=("p") # 2017 and 2018 dirs had 404 error
                            PXC_56_component_name=("percona-xtradb-cluster-full" "percona-xtradb-cluster-galera-3.x-")
                            PXC_56_component_repository=("testing" "main")
                            PXC_56_component_path=("percona-xtradb-cluster-5.6" "percona-xtradb-cluster-galera-3.x")

                            PXB_24=("pxb-24")
                            PXB_24_repo_version=("2" "2017" "2018" "7" "7.8" "7Server" "8" "8.0" "8.0" "8.2" "8Server")
                            PXB_24_component_name=("percona-xtrabackup-24-2.4")
                            PXB_24_component_repository=("testing" "experimental" "release" "laboratory")
                            PXB_24_component_path=("" "" "" "" "")

                            PXB_80=("pxb-80")
                            PXB_80_repo_version=("2" "2017" "2018" "7" "7.8" "7Server" "8" "8.0" "8.0" "8.2" "8Server")
                            PXB_80_component_name=("percona-xtrabackup-80-8.0")
                            PXB_80_component_repository=("testing" "experimental" "release" "laboratory")
                            PXB_80_component_path=("" "" "" "" "")

                            ##--------------------------------------DEBIAN------------------------------------------------

                            ##-------------------------------------APT-----------------------------------------




                            ##-------------------------------------APT-----------------------------------------



                            check_new_release_deb(){

                                    component=$1
                                    subpath=$2        
                                    version=$3
                                    repository=$4
                                    component_path=$5
                                            
                                    lftp -e "cls -1 > deb/$subpath-$version-$repository-apt; exit" "https://repo.percona.com/$version/apt/pool/$repository/$subpath/$component_path/"
                                    cat deb/$subpath-$version-$repository-apt | grep -i "$component" | sort > deb/release-$subpath-$version-$repository-$subpath-$component_path-$component
                                    rm -f deb/$subpath-$version-$repository-apt

                            }

                            driver_deb(){
                                declare -n PRODUCT=$1
                                declare -n REPO_VERSION=$2
                                declare -n COMPONENT_NAME=$3
                                declare -n REPOSITORY_NAME=$4
                                declare -n COMPONENT_PATH=$5

                                echo ${PRODUCT[@]}
                                echo ${REPO_VERSION[@]}
                                echo ${COMPONENT_NAME[@]}
                                echo ${REPOSITORY_NAME[@]}
                                echo ${COMPONENT_PATH[@]}

                                for h in ${REPOSITORY_NAME[@]}
                                do
                                    for i in ${PRODUCT[@]} 
                                    do
                                        for j in ${REPO_VERSION[@]} 
                                        do
                                            for k in ${COMPONENT_NAME[@]} 
                                            do
                                                for l in ${COMPONENT_PATH[@]} 
                                                do
                                                    check_new_release_deb $k $j $i $h $l
                                                done
                                            done
                                        done
                                    done
                                done

                            }
                            #-------------------------------------------DEB-----------------------------------------------
                            mkdir deb

                            LIST=("PXC_80" "PXC_56")

                            for a in ${LIST[@]}
                            do

                            driver_deb "$a" "${a}_repo_version" "${a}_component_name" "${a}_component_repository" "${a}_component_path"

                            done

                            echo """

                            -----------------------------------------------------
                            -----------------------------------------------------
                            -----------------------------------------------------
                            -----------------------------------------------------
                            -----------------------------------------------------

                            """
                            du -ksh deb/* | awk '{ print$2 }' | sort > /tmp/a-deb
                            du -ksh deb/* | grep "^0" | awk '{ print$2 }' | sort > /tmp/b-deb
                            diff /tmp/a-deb /tmp/b-deb | grep "<" | awk '{print$2}' > /tmp/diffed-deb

                            cat /tmp/diffed-deb
                            #-------------------------------------------RHEL-----------------------------------------------

                            FILES=$(cat /tmp/diffed-deb)

                            for i in $FILES
                            do 
                                echo "File name..."
                                echo "./$i"


                            done

                            '''

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














