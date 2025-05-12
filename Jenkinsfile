pipeline {
    agent any
    environment {
        DOCKER_HUB_CREDENTIALS = credentials('a80d8bfc-33ef-4152-a4ea-15237f18e593')
        DOCKER_IMAGE = 'cerulime/teedy'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        KUBE_NAMESPACE = 'teedy'
        KUBECONFIG_CREDENTIALS_ID = 'kubeconfig-file'
    }

    stages {
        stage('Setup Kubernetes') {
            steps {
                withCredentials([file(credentialsId: KUBECONFIG_CREDENTIALS_ID, variable: 'KUBECONFIG')]) {
                    sh 'mkdir -p $HOME/.kube'
                    sh 'cp $KUBECONFIG $HOME/.kube/config'
                }
            }
        }
        stage('Prepare Kubernetes Resources') {
            steps {
                script {
                    sh "kubectl create namespace ${KUBE_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
                    
                    sh """
                    kubectl create secret docker-registry regcred \
                        --docker-server=https://index.docker.io/v1/ \
                        --docker-username=${DOCKER_HUB_CREDENTIALS_USR} \
                        --docker-password=${DOCKER_HUB_CREDENTIALS_PSW} \
                        --namespace=${KUBE_NAMESPACE} \
                        --dry-run=client -o yaml | kubectl apply -f -
                    """
                }
            }
        }
        stage('Deploy Application') {
            steps {
                sh "kubectl apply -f teedy-deployment.yaml"

                sh "kubectl wait --for=condition=ready pod -l app=teedy -n ${KUBE_NAMESPACE} --timeout=120s"
            }
        }
        stage('Port Forwarding') {
            steps {
                script {
                    def pods = sh(script: "kubectl get pods -n ${KUBE_NAMESPACE} -l app=teedy -o jsonpath='{.items[*].metadata.name}'", returnStdout: true).trim().split()
                    
                    pods.eachWithIndex { pod, index ->
                        def localPort = 8081 + index
                        sh "kubectl port-forward -n ${KUBE_NAMESPACE} pod/${pod} ${localPort}:8080 &"
                        echo "Pod ${pod} 的8080端口已映射到本地${localPort}端口"
                    }
                }
            }
        }
    }
    post {
        always {
            sh 'pkill -f "kubectl port-forward" || true'
        }
    }
}