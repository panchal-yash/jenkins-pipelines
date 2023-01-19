def call(operatingSystems) {
  tests = [:]
  operatingSystems.each { os ->
   tests["${os}"] =  {
        stage("${os}") {

          echo "INSTALLING"

          env.BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${os}/bootstrap_instance_private_ip.json"
          env.COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${os}/common_instance_private_ip.json"

          env.BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/${product_to_test}/${os}/bootstrap_instance_public_ip.json"
          env.COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/${product_to_test}/${os}/common_instance_public_ip.json"

          sh """
            mkdir -p ${WORKSPACE}/${product_to_test}/${os}
          """
          moleculepxcPackageTestsINSTALL("${os}")

          echo "UPGRADING"

          env.BOOTSTRAP_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${os}/bootstrap_instance_private_ip.json"
          env.COMMON_INSTANCE_PRIVATE_IP = "${WORKSPACE}/${product_to_test}/${os}/common_instance_private_ip.json"

          env.BOOTSTRAP_INSTANCE_PUBLIC_IP = "${WORKSPACE}/${product_to_test}/${os}/bootstrap_instance_public_ip.json"
          env.COMMON_INSTANCE_PUBLIC_IP  = "${WORKSPACE}/${product_to_test}/${os}/common_instance_public_ip.json"

          sh """
            mkdir -p ${WORKSPACE}/${product_to_test}/${os}
          """
          moleculepxcPackageTestsUPGRADE("${os}")

        }
      }
    }
  parallel tests
}
