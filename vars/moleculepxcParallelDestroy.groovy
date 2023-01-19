def call(operatingSystems, product_to_test) {
  tests = [:]
  operatingSystems.each { os ->
   tests["${os}"] =  {
        stage("${os}") {

          moleculepxcRunMoleculeAction("destroy", params.product_to_test, "${os}", "install", params.test_repo, "yes")

        }
      }
    }
  parallel tests
}
