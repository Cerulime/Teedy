pipeline {
    agent any
    environment {
        DOCKER_HUB_CREDENTIALS = credentials('a80d8bfc-33ef-4152-a4ea-15237f18e593')
        DOCKER_IMAGE = 'cerulime/teedy'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/Cerulime/Teedy.git']]
                )
                sh 'mvn -B -DskipTests clean package'
            }
        }

        stage('Building image') {
            steps {
                script {
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    sh "echo ${DOCKER_HUB_CREDENTIALS_PSW} | docker login -u ${DOCKER_HUB_CREDENTIALS_USR} --password-stdin"
                    
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                    
                    sh 'docker logout'
                }
            }
        }

        stage('Run containers') {
            steps {
                script {
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'

                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8081 -d -p 8081:8080'
                    )

                    sh 'docker stop teedy-container-8082 || true'
                    sh 'docker rm teedy-container-8082 || true'

                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8082 -d -p 8082:8080'
                    )

                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
}