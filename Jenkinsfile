@Library('unifly-jenkins-common') _

pipeline {
    agent any

    triggers {
        cron '@weekly'
    }

    options {
        timestamps()
        buildDiscarder(
                logRotator(
                    // number of build logs to keep
                    numToKeepStr:'50',
                    // history to keep in days
                    daysToKeepStr: '60',
                    // number of builds have their artifacts kept
                    artifactNumToKeepStr: '1'
                )
            )
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
    }

    environment {
        GIT_REPO = 'https://github.com/unifly-aero/keycloak-sms-authenticator-sns.git'
        CREDENTIALS_ID = 'unifly-jenkins'
        JAVA_HOME="${tool 'openjdk-17'}"
        PATH="${env.JAVA_HOME}/bin:${tool 'nodejs-20'}/bin:${env.PATH}"
        ORG_GRADLE_PROJECT_uniflyVersionTargetBranch="${env.BRANCH_NAME}"

        UNIFLY_JFROG_ARTIFACTORY= credentials('artifactory-jenkins-cloud')
        uniflyJfrogUsername="$UNIFLY_JFROG_ARTIFACTORY_USR"
        uniflyJfrogPassword="$UNIFLY_JFROG_ARTIFACTORY_PSW"
    }

    stages {

        stage('Package') {
            steps {
                sh "./mvnw package"
            }
        }

        stage('Publish') {
            when { not { changeRequest() } }
            steps {
                  sh "./mvnw -s settings.xml deploy"
            }
        }
    }

    post {
        failure {
            sendSummary()
        }
        fixed {
            sendSummary()
        }
    }
}

