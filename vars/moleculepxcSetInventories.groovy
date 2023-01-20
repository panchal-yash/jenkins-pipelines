def call(String product_to_test, String node_to_test, String test_type){

        sh """

            echo \"Setting up Key path based on the selection\"

            if [[ (${node_to_test} == "ubuntu-focal")  ||  (${node_to_test} == "ubuntu-bionic") || (${node_to_test} == "ubuntu-jammy") ]];
            then
                SSH_USER="ubuntu"            
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${node_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${node_to_test}/ssh_key-us-west-2"
            elif [[ (${node_to_test} == "debian-11") ||  (${node_to_test} == "debian-10") ]];
            then
                SSH_USER="admin"            
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${node_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${node_to_test}/ssh_key-us-west-2"
            elif [[ (${node_to_test} == "ol-8") || (${node_to_test} == "ol-9") || (${node_to_test} == "min-amazon-2") ]];
            then
                SSH_USER="ec2-user"
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${node_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${node_to_test}/ssh_key-us-west-2"
            elif [[ (${node_to_test} == "centos-7") ]];
            then
                SSH_USER="centos"
                KEYPATH_BOOTSTRAP="/home/ec2-user/.cache/molecule/${product_to_test}-bootstrap/${node_to_test}/ssh_key-us-west-2"
                KEYPATH_COMMON="/home/ec2-user/.cache/molecule/${product_to_test}-common/${node_to_test}/ssh_key-us-west-2"
            else
                echo "OS Not yet in list of Keypath setup"
            fi

            echo \"printing path of bootstrap \$KEYPATH_BOOTSTRAP\"
            echo \"printing path of common  \$KEYPATH_COMMON\"
            echo \"printing user \$SSH_USER\"

            Bootstrap_Instance=\$(cat \${BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[])
            Bootstrap_Instance_Public_IP=\$(cat \${BOOTSTRAP_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[])
            
            export ip_env=\$Bootstrap_Instance
            
            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${node_to_test}/${product_to_test}-bootstrap/${test_type}/playbooks/
            echo "\n \$Bootstrap_Instance ansible_host=\$Bootstrap_Instance_Public_IP  ansible_ssh_user=\$SSH_USER ansible_ssh_private_key_file=\$KEYPATH_BOOTSTRAP ansible_ssh_common_args='-o StrictHostKeyChecking=no' ip_env=\$Bootstrap_Instance" > ${WORKSPACE}/package-testing/molecule/pxc/${node_to_test}/${product_to_test}-bootstrap/${test_type}/${operating_system}/playbooks/inventory

            export ip_env=\$Common_Instance_PXC2
            Common_Instance_PXC2=\$(cat \${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.instance] | jq -r .[])
            Common_Instance_PXC2_Public_IP=\$(cat \${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[0] | jq [.public_ip] | jq -r .[])

            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${node_to_test}/${product_to_test}-common/${test_type}/playbooks/
            echo "\n \$Common_Instance_PXC2 ansible_host=\$Common_Instance_PXC2_Public_IP   ansible_ssh_user=\$SSH_USER ansible_ssh_private_key_file=\$KEYPATH_COMMON ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=\$Common_Instance_PXC2" > ${WORKSPACE}/package-testing/molecule/pxc/${node_to_test}/${product_to_test}-common/${test_type}/${operating_system}/playbooks/inventory

            export ip_env=\$Common_Instance_PXC3
            Common_Instance_PXC3=\$(cat \${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.instance] | jq -r .[])
            Common_Instance_PXC3_Public_IP=\$(cat \${COMMON_INSTANCE_PUBLIC_IP} | jq -r .[1] | jq [.public_ip] | jq -r .[])

            mkdir -p ${WORKSPACE}/package-testing/molecule/pxc/${node_to_test}/${product_to_test}-common/${test_type}/playbooks/
            echo "\n \$Common_Instance_PXC3 ansible_host=\$Common_Instance_PXC3_Public_IP   ansible_ssh_user=\$SSH_USER ansible_ssh_private_key_file=\$KEYPATH_COMMON ansible_ssh_common_args='-o StrictHostKeyChecking=no'  ip_env=\$Common_Instance_PXC3" >> ${WORKSPACE}/package-testing/molecule/pxc/${node_to_test}/${product_to_test}-common/${test_type}/${operating_system}/playbooks/inventory
            """

}
