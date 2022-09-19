def call() {
    node('master') {
        unstash 'uploadPath'
        withCredentials([string(credentialsId: 'SIGN_PASSWORD', variable: 'SIGN_PASSWORD')]) {
            withCredentials([sshUserPrivateKey(credentialsId: '24e68886-c552-4033-8503-ed85bbaa31f3', keyFileVariable: 'KEY_PATH', passphraseVariable: '', usernameVariable: 'USER')]) {
                sh """
                    export path_to_build=`cat uploadPath`

                    ssh -o StrictHostKeyChecking=no -i ${KEY_PATH} ${USER}@repo.ci.percona.com " \
                        ls \${path_to_build}/binary/redhat/*/*/*.rpm \
                            | xargs -n 1 signpackage --verbose --password ${SIGN_PASSWORD} --rpm
                    "
                """
            }
        }
    }
}
