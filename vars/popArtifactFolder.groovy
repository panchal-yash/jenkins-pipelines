def call(String FOLDER_NAME, String AWS_STASH_PATH) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: '24e68886-c552-4033-8503-ed85bbaa31f3', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """
            S3_PATH=s3://percona-jenkins-artifactory/${AWS_STASH_PATH}
            aws s3 cp --quiet --recursive \$S3_PATH/${FOLDER_NAME} ${FOLDER_NAME} || :
        """
    }
}

