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
        sh """
            source venv/bin/activate
            export MOLECULE_DEBUG=1
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
            echo 'INSTANCE_PRIVATE_IP: "${BOOTSTRAP_INSTANCE_PRIVATE_IP}"' > envfile
            echo 'INSTANCE_PUBLIC_IP: "${BOOTSTRAP_INSTANCE_PUBLIC_IP}"' >> envfile
            molecule -e envfile ${action} -s ${scenario}
            cd -

            cd ${product_to_test}-common
            echo "INSTANCE_PRIVATE_IP: "${COMMON_INSTANCE_PRIVATE_IP}"" > envfile_common
            echo "INSTANCE_PUBLIC_IP: "${COMMON_INSTANCE_PUBLIC_IP}"" >> envfile_common
            molecule -e envfile_common ${action} -s ${scenario}
            cd -
        """

       sh """mkdir -p ${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/"""



        BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/bootstrap_instance_private_ip.json"
        COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/common_instance_private_ip.json"

        BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/bootstrap_instance_public_ip.json"
        COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/${product_to_test}/${scenario}/${test_type}/common_instance_public_ip.json"

        PXC1_IP = sh(
            script: """jq -r \'.[] | select(.instance | startswith("pxc1")).private_ip\' ${BOOTSTRAP_INSTANCE_PRIVATE_IP}""",
            returnStdout: true
        ).trim()
        PXC2_IP = sh(
            script: """jq -r \'.[] | select(.instance | startswith("pxc2")).private_ip\' ${COMMON_INSTANCE_PRIVATE_IP}""",
            returnStdout: true
        ).trim()
        PXC3_IP = sh(
            script: """jq -r \'.[] | select(.instance | startswith("pxc3")).private_ip\' ${COMMON_INSTANCE_PRIVATE_IP}""",
            returnStdout: true
        ).trim()

        sh """
            echo ${PXC1_IP} > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/pxc1
            echo ${PXC2_IP} > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/pxc2
            echo ${PXC3_IP} > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/pxc3
        """

 
//--- Move the PXC1_IP values to a file


//--- Setting up Keypath based on selection


            echo "Setting up Key path based on the selection"

            if ("${product_to_test}" == "ubuntu-focal"  ||  "${product_to_test}" == "ubuntu-bionic" || "${product_to_test}" == "ubuntu-jammy"){
                SSH_USER="ubuntu"            
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/"${product_to_test}"-bootstrap/"${product_to_test}"/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/"${product_to_test}"-common/"${product_to_test}"/ssh_key-us-west-2"
            }
            else if("${product_to_test}" == "debian-11" || "${product_to_test}" == "debian-10"){
                SSH_USER="admin"            
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/"${product_to_test}"-bootstrap/"${product_to_test}"/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/"${product_to_test}"-common/"${product_to_test}"/ssh_key-us-west-2"
            }
            else if("${product_to_test}" == "ol-8" || "${product_to_test}" == "ol-9" || "${product_to_test}" == "min-amazon-2"){
                SSH_USER="ec2-user"
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/"${product_to_test}"-bootstrap/"${product_to_test}"/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/"${product_to_test}"-common/"${product_to_test}"/ssh_key-us-west-2"
            }
            else if("${product_to_test}" == "centos-7"){
                SSH_USER="centos"
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/"${product_to_test}"-bootstrap/"${product_to_test}"/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/"${product_to_test}"-common/"${product_to_test}"/ssh_key-us-west-2"
            }
            else
            {
                echo "OS Not yet in list of Keypath setup"
            }



            Bootstrap_Instance = sh(
                script: """echo ${BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Bootstrap_Instance_Public_IP = sh(
                script: """echo ${BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC2 = sh(
                script: """echo ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC2_Public_IP = sh(
                script: """echo ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC3 = sh(
                script: """echo ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.instance] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC3_Public_IP = sh(
                script: """echo ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.public_ip] | jq -r .[]""",
                returnStdout: true
            ).trim()


//---- SET INVENTORIES ----

            sh """

            echo \"printing path of bootstrap ${KEYPATH_BOOTSTRAP}"
            echo \"printing path of common  ${KEYPATH_COMMON}"
            echo \"printing user ${SSH_USER}"

            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-bootstrap/${test_type}/${operating_system}/playbooks/
            echo "\n ${Bootstrap_Instance} ansible_host=${Bootstrap_Instance_Public_IP}  ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_BOOTSTRAP} ansible_ssh_common_args='-o StrictHostKeyChecking=no' ip_env=${Bootstrap_Instance}" > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-bootstrap/${test_type}/${operating_system}/playbooks/inventory

            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${operating_system}/playbooks/
            echo "\n ${Common_Instance_PXC2} ansible_host=${Common_Instance_PXC2_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${Common_Instance_PXC2}" > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${operating_system}/playbooks/inventory
            
            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${operating_system}/playbooks/
            echo "\n ${Common_Instance_PXC3} ansible_host=${Common_Instance_PXC3_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${Common_Instance_PXC3}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${operating_system}/playbooks/inventory
            
            """

//----- SET INVENTORIES ----


    }
}
