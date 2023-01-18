def call(String operating_system){
                echo "1. Creating Molecule Instances for running INSTALL PXC tests.. Molecule create step"

                runMoleculeAction("create", params.product_to_test, operating_system, "install", params.test_repo, "yes")
                setInstancePrivateIPEnvironment()

                echo "2. Run Install scripts and tests for PXC INSTALL PXC tests.. Molecule converge step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        runMoleculeAction("converge", params.product_to_test, operating_system, "install", params.test_repo, "yes")
                    }
                }

                 echo "3. Take Backups of the Logs.. PXC INSTALL tests.."

                setInventories()
                runlogsbackup(params.product_to_test, "INSTALL")

                echo "4. Destroy the Molecule instances for the PXC INSTALL tests.."

                runMoleculeAction("destroy", params.product_to_test, operating_system, "install", params.test_repo, "yes")
}
