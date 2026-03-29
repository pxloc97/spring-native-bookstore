#!/bin/sh

set -eu

KIND_CLUSTER="${KIND_CLUSTER:-bookstore}"
KUBE_CONTEXT="kind-${KIND_CLUSTER}"

printf "\nApplying platform manifests...\n"
kubectl create configmap keycloak-realm-config \
  --context "$KUBE_CONTEXT" \
  --from-file=realm-config.json=polar-deployment/docker/keycloak/realm-config.json \
  --dry-run=client -o yaml | kubectl apply --context "$KUBE_CONTEXT" -f -

kubectl apply --context "$KUBE_CONTEXT" \
  -f polar-deployment/kubernetes/local/postgresql.yml \
  -f polar-deployment/kubernetes/local/postgresql-order.yml \
  -f polar-deployment/kubernetes/local/keycloak.yml \
  -f polar-deployment/kubernetes/local/redis.yml \
  -f polar-deployment/kubernetes/local/rabbitmq.yml

printf "\nWaiting for platform deployments...\n"
kubectl wait --context "$KUBE_CONTEXT" --for=condition=available deployment/polar-postgres --timeout=180s
kubectl wait --context "$KUBE_CONTEXT" --for=condition=available deployment/polar-postgres-order --timeout=180s
kubectl wait --context "$KUBE_CONTEXT" --for=condition=available deployment/polar-keycloak --timeout=300s
kubectl wait --context "$KUBE_CONTEXT" --for=condition=available deployment/polar-redis --timeout=180s
kubectl wait --context "$KUBE_CONTEXT" --for=condition=available deployment/polar-rabbitmq --timeout=180s

printf "\nPlatform is ready on context %s\n" "$KUBE_CONTEXT"
