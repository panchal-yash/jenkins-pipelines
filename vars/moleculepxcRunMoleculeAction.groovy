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
            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/${test_type}/
            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/
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

                echo 'INSTANCE_PRIVATE_IP: "${BOOTSTRAP_INSTANCE_PRIVATE_IP}"' > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/${test_type}/envfile
                echo 'INSTANCE_PUBLIC_IP: "${BOOTSTRAP_INSTANCE_PUBLIC_IP}"' >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/${test_type}/envfile
                echo "INSTANCE_PRIVATE_IP: "${COMMON_INSTANCE_PRIVATE_IP}"" > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/envfile
                echo "INSTANCE_PUBLIC_IP: "${COMMON_INSTANCE_PUBLIC_IP}"" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/envfile

                cd ${product_to_test}-bootstrap
                molecule -e ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/${test_type}/envfile ${action} -s ${scenario}
                cd -

                cd ${product_to_test}-common
                molecule -e ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/envfile ${action} -s ${scenario}
                cd -
            """

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
                    echo "PXC1: ${PXC1_IP}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/${test_type}/envfile
                    echo "PXC2: ${PXC2_IP}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/envfile
                    echo "PXC3: ${PXC3_IP}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/envfile
                    echo "${PXC1_IP}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-bootstrap/molecule/${scenario}/${test_type}/PXC1
                    echo "${PXC2_IP}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/PXC2
                    echo "${PXC3_IP}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}-common/molecule/${scenario}/${test_type}/PXC3
                """
 
//--- Move the PXC1_IP values to a file


//--- Setting up Keypath based on selection


            echo "Setting up Key path based on the selection"

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

            Bootstrap_Instance = sh(
                script: """cat ${BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Bootstrap_Instance_Public_IP = sh(
                script: """cat ${BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC2 = sh(
                script: """cat ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC2_Public_IP = sh(
                script: """cat ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC3 = sh(
                script: """cat ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.instance] | jq -r .[]""",
                returnStdout: true
            ).trim()

            Common_Instance_PXC3_Public_IP = sh(
                script: """cat ${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.public_ip] | jq -r .[]""",
                returnStdout: true
            ).trim()


//---- SET INVENTORIES ----

            sh """

            echo \"printing path of bootstrap ${KEYPATH_BOOTSTRAP}"
            echo \"printing path of common  ${KEYPATH_COMMON}"
            echo \"printing user ${SSH_USER}"

            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-bootstrap/${test_type}/${scenario}/playbooks/
            echo "\n ${Bootstrap_Instance} ansible_host=${Bootstrap_Instance_Public_IP}  ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_BOOTSTRAP} ansible_ssh_common_args='-o StrictHostKeyChecking=no' ip_env=${Bootstrap_Instance}" > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-bootstrap/${test_type}/${scenario}/playbooks/inventory

            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${scenario}/playbooks/
            echo "\n ${Common_Instance_PXC2} ansible_host=${Common_Instance_PXC2_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${Common_Instance_PXC2}" > ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${scenario}/playbooks/inventory
            
            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${scenario}/playbooks/
            echo "\n ${Common_Instance_PXC3} ansible_host=${Common_Instance_PXC3_Public_IP}   ansible_ssh_user=${SSH_USER} ansible_ssh_private_key_file=${KEYPATH_COMMON} ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=${Common_Instance_PXC3}" >> ${WORKSPACE}/package-testing/molecule/pxc/${product_to_test}/${product_to_test}-common/${test_type}/${scenario}/playbooks/inventory
            
            """

//----- SET INVENTORIES ----

            echo "Completing the Molecule Action"

    }
}
