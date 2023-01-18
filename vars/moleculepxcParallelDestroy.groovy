def call(operatingSystems, product_to_test) {
  tests = [:]
  operatingSystems.each { os ->
   tests["${os}"] =  {
        stage("${os}") {
          
          echo "DESTROYING"
          sh """
            cd package-testing/molecule/pxc

            cd ${product_to_test}-bootstrap
            export INSTANCE_PRIVATE_IP=\${BOOTSTRAP_INSTANCE_PRIVATE_IP}
            export INSTANCE_PUBLIC_IP=\${BOOTSTRAP_INSTANCE_PUBLIC_IP}            
            molecule destroy -s ${os}
            cd -

            cd ${product_to_test}-common
            export INSTANCE_PRIVATE_IP=\${COMMON_INSTANCE_PRIVATE_IP}
            export INSTANCE_PUBLIC_IP=\${COMMON_INSTANCE_PUBLIC_IP}        
            molecule destroy -s ${os}
            cd -
          """
        }
      }
    }
  parallel tests
}
