pipeline {
  environment {
    GH_CREDS = credentials('jenkins-x-github')
  }
  agent {
    label "jenkins-maven"
  }
  stages {
    stage('CI Build') {
      when {
        branch 'PR-*'
      }
      steps {
        checkout scm
        container('maven') {
          sh "mvn clean install"
        }
      }
    }

    stage('Build and Push Release') {
      when {
        branch 'master'
      }
      steps {
        checkout scm
        container('maven') {
          // until kubernetes plugin supports init containers https://github.com/jenkinsci/kubernetes-plugin/pull/229/
          sh 'cp /root/netrc/.netrc ~/.netrc'

          sh "git checkout master"
          sh "jx-release-version > /tmp/version"

          sh "git branch release-\$(cat /tmp/version)"
          sh "git checkout release-\$(cat /tmp/version)"

          sh "mvn versions:set -DnewVersion=\$(cat /tmp/version)"
          sh "mvn deploy"


          sh "git commit -a -m 'release \$(cat /tmp/version)'"
          sh "git push origin release-\$(cat /tmp/version)"
        }
      }
    }
  }
}
