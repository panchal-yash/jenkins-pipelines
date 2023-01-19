void call(String product_to_test, String test_phase, String operating_system) {
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
            export test_phase=${test_phase}
            echo $test_phase

            echo "Running the logs backup task for pxc bootstrap node"
            ansible-playbook ${WORKSPACE}/package-testing/molecule/pxc/playbooks/logsbackup.yml -i ${WORKSPACE}/package-testing/molecule/pxc/${params.node_to_test}/${product_to_test}-bootstrap/${operating_system}/playbooks/inventory

            echo "Running the logs backup task for pxc common node"
            ansible-playbook ${WORKSPACE}/package-testing/molecule/pxc/playbooks/logsbackup.yml -i ${WORKSPACE}/package-testing/molecule/pxc/${params.node_to_test}/${product_to_test}-common/${operating_system}/playbooks/inventory
        """
    }
}
