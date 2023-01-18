void pxcPackageTestsINSTALL(String operating_system){
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

void pxcPackageTestsUPGRADE(String operating_system){

                echo "1. Creating Molecule Instances for running PXC UPGRADE tests.. Molecule create step"

                runMoleculeAction("create", params.product_to_test, operating_system, "upgrade", "main", "no")
                setInstancePrivateIPEnvironment()

                echo "2. Run Install scripts and tests for running PXC UPGRADE tests.. Molecule converge step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        runMoleculeAction("converge", params.product_to_test, operating_system, "upgrade", "main", "no")
                    }
                }

                echo "3. Run UPGRADE scripts and playbooks for running PXC UPGRADE tests.. Molecule side-effect step"

                script{
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE'){
                        runMoleculeAction("side-effect", params.product_to_test, operating_system, "upgrade", params.test_repo, "yes")
                    }
                }

                echo "4. Take Backups of the Logs.. for PXC UPGRADE tests"

                setInventories()
                runlogsbackup(params.product_to_test, "UPGRADE")

                echo "5. Destroy the Molecule instances for PXC UPGRADE tests.."

                runMoleculeAction("destroy", params.product_to_test, operating_system, "upgrade", params.test_repo, "yes")



}


def pxcPackageTestsALL(String operating_system) {
    
    pxcPackageTestsINSTALL(operating_system)
    pxcPackageTestsUPGRADE(operating_system)

}


