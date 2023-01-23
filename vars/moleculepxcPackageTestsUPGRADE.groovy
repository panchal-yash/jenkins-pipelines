def call(String operating_system){

                echo "1. Creating Molecule Instances for running PXC UPGRADE tests.. Molecule create step"

                moleculepxcRunMoleculeAction("create", params.product_to_test, operating_system, "upgrade", "main", "no")

                echo "2. Run Install scripts and tests for running PXC UPGRADE tests.. Molecule converge step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        moleculepxcRunMoleculeAction("converge", params.product_to_test, operating_system, "upgrade", "main", "no")
                    }
                }

                echo "3. Run UPGRADE scripts and playbooks for running PXC UPGRADE tests.. Molecule side-effect step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        moleculepxcRunMoleculeAction("side-effect", params.product_to_test, operating_system, "upgrade", params.test_repo, "yes")
                    }
                }

                echo "4. Take Backups of the Logs.. for PXC UPGRADE tests"

                moleculepxcRunLogsBackup(params.product_to_test, "UPGRADE", operating_system)

                echo "5. Destroy the Molecule instances for PXC UPGRADE tests.."

                moleculepxcRunMoleculeAction("destroy", params.product_to_test, operating_system, "upgrade", params.test_repo, "yes")

}
