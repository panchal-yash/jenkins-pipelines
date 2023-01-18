void call(String action, String product_to_test, String scenario, String test_type, String test_repo, String version_check) {
    def awsCredentials = [
        sshUserPrivateKey(
            credentialsId: 'MOLECULE_AWS_PRIVATE_KEY',
            keyFileVariable: 'MOLECULE_AWS_PRIVATE_KEY',
            passphraseVariable: '',
            usernameVariable: ''
        ),
        aws(
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            credentialsId: '5d78d9c7-2188-4b16-8e31-4d5782c6ceaa',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        )
    ]

    withCredentials(awsCredentials) {
        sh """
            source venv/bin/activate
            export MOLECULE_DEBUG=0
            export test_repo=${test_repo}
            export test_type=${test_type}
            
            if [[ ${product_to_test} = "pxc57" ]];
            then
                export pxc57repo=${params.pxc57_repo}
            else
                echo "Product is not pxc57 so skipping value assignment to it"
            fi
            
	        if [[ ${test_type} = "install" ]];
            then
                export install_repo=${test_repo}
                export check_version="${version_check}"
            elif [[ ${test_type} == "upgrade" ]]
            then
                export install_repo="main"
                export check_version="${version_check}"
                export upgrade_repo=${test_repo}
            else
                echo "Unknown condition"
            fi

            cd package-testing/molecule/pxc

            cd ${product_to_test}-bootstrap
            export INSTANCE_PRIVATE_IP=\${BOOTSTRAP_INSTANCE_PRIVATE_IP}
            export INSTANCE_PUBLIC_IP=\${BOOTSTRAP_INSTANCE_PUBLIC_IP}            
            molecule ${action} -s ${scenario}
            cd -

            cd ${product_to_test}-common
            export INSTANCE_PRIVATE_IP=\${COMMON_INSTANCE_PRIVATE_IP}
            export INSTANCE_PUBLIC_IP=\${COMMON_INSTANCE_PUBLIC_IP}        
            molecule ${action} -s ${scenario}
            cd -
        """
    }
}
