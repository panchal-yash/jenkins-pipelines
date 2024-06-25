def call(operatingSystems, moleculeDir, test) {
  tests = [:]
  operatingSystems.each { os ->
   tests["${os}"] =  {
        stage("${os}-${test}") {
            sh """
                  . virtenv/bin/activate
                  cd ${moleculeDir}
                  ls -la
                  find . -type f -exec sed -i "s/TEST/${test}/g" {} +
                  molecule test -s ${os}
               """
        }
      }
    }
  parallel tests
}
