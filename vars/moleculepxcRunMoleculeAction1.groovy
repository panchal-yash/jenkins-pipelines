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

        sh """
            mkdir -p ${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/
        """

        BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/bootstrap_instance_private_ip.json"
        COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/common_instance_private_ip.json"

        BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/bootstrap_instance_public_ip.json"
        COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/common_instance_public_ip.json"

    withCredentials(awsCredentials) {

            echo "Setting up Key path based on the selection"
            echo "${scenario}"

            if ( scenario == "ubuntu-focal"  ||  scenario == "ubuntu-bionic" || scenario == "ubuntu-jammy"){
                SSH_USER="ubuntu"            
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${product_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${product_to_test}/ssh_key-us-west-2"
            }
            else if( scenario == "debian-11" || scenario == "debian-10"){
                SSH_USER="admin"            
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${product_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${product_to_test}/ssh_key-us-west-2"
            }
            else if( scenario == "ol-8" || scenario == "ol-9" || scenario == "min-amazon-2"){
                SSH_USER="ec2-user"
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${product_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${product_to_test}/ssh_key-us-west-2"
            }
            else if( scenario == "centos-7"){
                SSH_USER="centos"
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${product_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${product_to_test}/ssh_key-us-west-2"
            }
            else
            {
                echo "OS Not yet in list of Keypath setup"
            }


            echo "${SSH_USER}"
            echo "${KEYPATH_BOOTSTRAP}"
            echo "${KEYPATH_COMMON}"

    }
}
