def call(String operating_system){
                echo "1. Creating Molecule Instances for running INSTALL PXC tests.. Molecule create step"

                moleculepxcRunMoleculeAction("create", params.product_to_test, operating_system, "install", params.test_repo, "yes")
                moleculepxcSetInstancePrivateIPEnvironment()

                echo "2. Run Install scripts and tests for PXC INSTALL PXC tests.. Molecule converge step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        moleculepxcRunMoleculeAction("converge", params.product_to_test, operating_system, "install", params.test_repo, "yes")
                    }
                }

                echo "3. Take Backups of the Logs.. PXC INSTALL tests.."

                echo "3.1 Set Inventories for INSTALL"
                moleculepxcSetInventories(params.product_to_test, operating_system, "install")

                echo "3.2 Run Molecule Log Backups INSTALL"
                moleculepxcRunLogsBackup(params.product_to_test, "install", operating_system)

                echo "4. Destroy the Molecule instances for the PXC INSTALL tests.."

                moleculepxcRunMoleculeAction("destroy", params.product_to_test, operating_system, "install", params.test_repo, "yes")
}
