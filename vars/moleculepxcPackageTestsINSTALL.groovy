def call(String operating_system){
                echo "1. Creating Molecule Instances for running INSTALL PXC tests.. Molecule create step"
                sh "rm -rf /home/ec2-user/.cache/molecule/pxc80-bootstrap/${operating_system}/"
                moleculepxcRunMoleculeAction("create", params.product_to_test, operating_system, "install", params.test_repo, "yes")

                echo "2. Run Install scripts and tests for PXC INSTALL PXC tests.. Molecule converge step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        moleculepxcRunMoleculeAction("converge", params.product_to_test, operating_system, "install", params.test_repo, "yes")
                    }
                }
                
                echo "3.2 Run Molecule Log Backups INSTALL"
                moleculepxcRunLogsBackup(params.product_to_test, "install", operating_system)

                echo "4. Destroy the Molecule instances for the PXC INSTALL tests.."

                moleculepxcRunMoleculeAction("destroy", params.product_to_test, operating_system, "install", params.test_repo, "yes")
                sh "rm -rf /home/ec2-user/.cache/molecule/pxc80-bootstrap/${operating_system}/"
}
