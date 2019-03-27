#!groovy

/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */

def isPullRequest = env.BRANCH_NAME.startsWith('PR-')

// constants will be used for testing
def JENKINS_CREDENTIAL = 'jenkins-credential'
def GITHUB_USERNAME = 'Zowe Robot'
def GITHUB_EMAIL = 'zowe.robot@gmail.com'
def GITHUB_CREDENTIAL = 'zowe-robot-github'
def NPM_EMAIL = 'giza-jenkins@gmail.com'
def NPM_CREDENTIAL= 'giza-jenkins-basicAuth'

def opts = []
// keep last 20 builds for regular branches, no keep for pull requests
opts.push(buildDiscarder(logRotator(numToKeepStr: (isPullRequest ? '' : '20'))))
// disable concurrent build
opts.push(disableConcurrentBuilds())

// define custom build parameters
def customParameters = []
customParameters.push(choice(
    name: 'TEST_LOG_LEVEL',
    choices: ['INFO', 'SEVERE', 'WARNING', 'CONFIG', 'FINE', 'FINER', 'FINEST'],
    description: 'Log level for running gradle test'
))
opts.push(parameters(customParameters))

// set build properties
properties(opts)

// load local files as library
def repoPath = sh(returnStdout: true, script: 'pwd').trim()
library(
  identifier: 'local-lib@master',
  retriever: modernSCM([$class: 'GitSCMSource', remote: repoPath])
)

node ('ibm-jenkins-slave-nvm-jnlp') {
    stage('checkout') {
        // checkout source code
        checkout scm

        // check if it's pull request
        echo "Current branch is ${env.BRANCH_NAME}"
        if (isPullRequest) {
          echo "This is a pull request"
        }
    }

    stage('github') {
      def github = new org.zowe.scm.GitHub(this, [
        'repository'                 : 'zowe/jenkins-library-fvt-nodejs',
        'username'                   : GITHUB_USERNAME,
        'email'                      : GITHUB_EMAIL,
        'usernamePasswordCredential' : System.getProperty('github.credential'),
      ])
      github.clone(['targetFolder': '.tmp-git'])
      sh 'ls -la .tmp-git'
      error 'exit here'
    }

    stage('test') {
        try {
            withCredentials([usernamePassword(
                credentialsId: JENKINS_CREDENTIAL,
                passwordVariable: 'PASSWORD',
                usernameVariable: 'USERNAME'
            )]) {
                sh """
./gradlew test \
  -PlogLevel=${params.TEST_LOG_LEVEL} \
  -Pjenkins.baseuri=${env.JENKINS_URL} \
  -Pjenkins.user=${USERNAME} \
  -Pjenkins.password=${PASSWORD} \
  -Plibrary.branch=${env.BRANCH_NAME} \
  -Pgithub.username=${GITHUB_USERNAME} \
  -Pgithub.email=${GITHUB_EMAIL} \
  -Pgithub.credential=${GITHUB_CREDENTIAL}
  -Pnpm.email=${NPM_EMAIL} \
  -Pnpm.credential=${NPM_CREDENTIAL}
"""
            }
        } catch (e) {
            throw e
        } finally {
            // publish any test results found
            junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'

            // publish test html report
            publishHTML(target: [
                allowMissing         : false,
                alwaysLinkToLastBuild: false,
                keepAll              : true,
                reportDir            : 'build/reports/tests/test',
                reportFiles          : 'index.html',
                reportName           : "Unit Test Results"
            ])
        }
    }

    stage('render-doc') {
        sh './gradlew groovydoc'
    }
}
