def call(operatingSystems) {
  tests = [:]
  operatingSystems.each { os ->
   tests["${os}"] =  {
        stage("${os}") {

          echo "INSTALLING"
          moleculepxcPackageTestsINSTALL("${os}")
          echo "UPGRADING"
          moleculepxcPackageTestsUPGRADE("${os}")

        }
      }
    }
  parallel tests
}
