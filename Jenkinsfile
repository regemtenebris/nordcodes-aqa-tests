// Declarative pipeline for Jenkins.
// Required plugins: Pipeline, JUnit, Allure Jenkins Plugin (tool name "allure" in Global Tool Configuration).
pipeline {
    agent any

    tools {
        jdk 'jdk17'              // name of a JDK 17 installation in "Global Tool Configuration"
    }

    parameters {
        booleanParam(name: 'RUN_KNOWN_BUGS', defaultValue: true,
                description: 'Also run tests that reproduce already reported bugs (marks build UNSTABLE)')
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Regression') {
            steps {
                // Must be green. Test failures fail the build (red).
                sh './mvnw -B clean test -DexcludedGroups=known-bug'
            }
        }

        stage('Known bugs') {
            when { expression { params.RUN_KNOWN_BUGS } }
            steps {
                // Expected failures: the build becomes UNSTABLE (yellow), not FAILED (red).
                sh './mvnw -B test -Dgroups=known-bug -Dmaven.test.failure.ignore=true'
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
            allure results: [[path: 'target/allure-results']]
            archiveArtifacts artifacts: 'target/app-under-test.log', allowEmptyArchive: true
        }
    }
}
