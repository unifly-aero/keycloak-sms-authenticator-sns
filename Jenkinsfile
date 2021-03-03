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
        JAVA_HOME="${tool 'openjdk-11'}"
        PATH="${env.JAVA_HOME}/bin:${tool 'nodejs-12'}/bin:${env.PATH}"
        ORG_GRADLE_PROJECT_uniflyVersionTargetBranch="${env.BRANCH_NAME}"
        UNIFLY_ARTIFACTORY = credentials('unifly-artifactory')
        ORG_GRADLE_PROJECT_artifactoryUser = "$UNIFLY_ARTIFACTORY_USR"
        ORG_GRADLE_PROJECT_artifactoryPassword = "$UNIFLY_ARTIFACTORY_PSW"
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
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'unifly-artifactory', passwordVariable: 'artifactory_password', usernameVariable: 'artifactory_user']]) {
                  sh "./mvnw -s settings.xml deploy"
                }
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
