pipeline {
  agent {
    label "jenkins-maven"
  }
  stages {
    stage('Maven Release') {
      steps {
        mavenFlow {
          cdOrganisation "jenkins-x"
          useStaging false
          useSonatype true
          pauseOnFailure true
        }
      }
    }
  }
}
