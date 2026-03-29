#!/bin/sh

set -eu

MINIKUBE_PROFILE="${MINIKUBE_PROFILE:-polar}"
KUBE_CONTEXT="${KUBE_CONTEXT:-$MINIKUBE_PROFILE}"

printf "\n📦 Initializing Kubernetes cluster...\n\n"
minikube start --cpus 2 --memory 4g --driver docker --profile "$MINIKUBE_PROFILE"

printf "\n🔌 Enabling NGINX Ingress Controller...\n\n"
minikube addons enable ingress --profile "$MINIKUBE_PROFILE"

sleep 30

printf "\n📦 Creating Keycloak realm config...\n"
kubectl create configmap keycloak-realm-config \
  --context "$KUBE_CONTEXT" \
  --from-file=realm-config.json=polar-deployment/docker/keycloak/realm-config.json \
  --dry-run=client -o yaml | kubectl apply --context "$KUBE_CONTEXT" -f -

printf "\n📦 Deploying Keycloak...\n"
kubectl apply --context "$KUBE_CONTEXT" -f polar-deployment/kubernetes/local/keycloak.yml

printf "\n⌛ Waiting for Keycloak to be ready...\n"
kubectl wait --context "$KUBE_CONTEXT" \
  --for=condition=available deployment/polar-keycloak \
  --timeout=300s

printf "\n📦 Deploying PostgreSQL (catalog and order)...\n"
kubectl apply --context "$KUBE_CONTEXT" -f polar-deployment/kubernetes/local/postgresql.yml
kubectl apply --context "$KUBE_CONTEXT" -f polar-deployment/kubernetes/local/postgresql-order.yml

printf "\n⌛ Waiting for PostgreSQL deployments to be ready...\n"
kubectl wait --context "$KUBE_CONTEXT" \
  --for=condition=available deployment/polar-postgres \
  --timeout=180s
kubectl wait --context "$KUBE_CONTEXT" \
  --for=condition=available deployment/polar-postgres-order \
  --timeout=180s

printf "\n📦 Deploying Redis...\n"
kubectl apply --context "$KUBE_CONTEXT" -f polar-deployment/kubernetes/local/redis.yml

printf "\n⌛ Waiting for Redis to be ready...\n"
kubectl wait --context "$KUBE_CONTEXT" \
  --for=condition=available deployment/polar-redis \
  --timeout=180s

printf "\n📦 Deploying RabbitMQ...\n"
kubectl apply --context "$KUBE_CONTEXT" -f polar-deployment/kubernetes/local/rabbitmq.yml

printf "\n⌛ Waiting for RabbitMQ to be ready...\n"
kubectl wait --context "$KUBE_CONTEXT" \
  --for=condition=available deployment/polar-rabbitmq \
  --timeout=180s

printf "\n📦 Deploying Polar UI service...\n"
kubectl apply --context "$KUBE_CONTEXT" -f polar-deployment/kubernetes/local/polar-ui.yml

printf "\n⛵ Happy Sailing!\n\n"
