def call() {
    env.PXC1_IP = sh(
        script: 'jq -r \'.[] | select(.instance | startswith("pxc1")).private_ip\' ${BOOTSTRAP_INSTANCE_PRIVATE_IP}',
        returnStdout: true
    ).trim()
    env.PXC2_IP = sh(
        script: 'jq -r \'.[] | select(.instance | startswith("pxc2")).private_ip\' ${COMMON_INSTANCE_PRIVATE_IP}',
        returnStdout: true
    ).trim()
    env.PXC3_IP = sh(
        script: 'jq -r \'.[] | select(.instance | startswith("pxc3")).private_ip\' ${COMMON_INSTANCE_PRIVATE_IP}',
        returnStdout: true
    ).trim()
}
