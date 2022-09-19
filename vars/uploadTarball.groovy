def call(String TarballType) {
    node('master') {
        deleteDir()
        unstash "${TarballType}.tarball"
        unstash 'uploadPath'
        withCredentials([sshUserPrivateKey(credentialsId: '24e68886-c552-4033-8503-ed85bbaa31f3', keyFileVariable: 'KEY_PATH', usernameVariable: 'USER')]) {
            sh """
                export path_to_build=`cat uploadPath`

                ssh -o StrictHostKeyChecking=no -i ${KEY_PATH} ${USER}@repo.ci.percona.com \
                    mkdir -p \${path_to_build}/${TarballType}/tarball

                scp -o StrictHostKeyChecking=no -i ${KEY_PATH} \
                    `find . -name '*.tar.*'` \
                    ${USER}@repo.ci.percona.com:\${path_to_build}/${TarballType}/tarball/

            """
        }
        deleteDir()
    }
}
