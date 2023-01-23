def call(operatingSystems, String test_type) {
  tests = [:]
  operatingSystems.each { os ->
   tests["${os}"] =  {
        stage("${os}") {

          if (test_type == "install" || test_type === "install_and_upgrade"){

            echo "INSTALLING"
            moleculepxcPackageTestsINSTALL("${os}")

          }
          else if (test_type == "upgrade" || test_type === "install_and_upgrade"){

            echo "UPGRADING"
            moleculepxcPackageTestsUPGRADE("${os}")

          }

        }
      }
    }
  parallel tests
} 
