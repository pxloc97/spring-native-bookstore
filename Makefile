SHELL := /bin/bash

SERVICES := config catalog order edge
SERVICE_DIR_config := config-service
SERVICE_DIR_catalog := catalog-service
SERVICE_DIR_order := order-service
SERVICE_DIR_edge := edge-service

KIND_CLUSTER ?= bookstore
KIND_CONFIG := polar-deployment/kubernetes/local/kind-config.yml
KUBE_CONTEXT := kind-$(KIND_CLUSTER)
K8S_PLATFORM_FILES := \
	polar-deployment/kubernetes/local/postgresql.yml \
	polar-deployment/kubernetes/local/postgresql-order.yml \
	polar-deployment/kubernetes/local/redis.yml
EDGE_K8S_FILES := edge-service/k8s/deployment.yml edge-service/k8s/service.yml edge-service/k8s/ingress.yml
SKAFFOLD_FILE := skaffold.yml
INGRESS_NGINX_VERSION := controller-v1.11.3
INGRESS_NGINX_MANIFEST := https://raw.githubusercontent.com/kubernetes/ingress-nginx/$(INGRESS_NGINX_VERSION)/deploy/static/provider/kind/deploy.yaml

.PHONY: help build test clean \
	build-% test-% clean-% run-% \
	platform-up platform-down edge-up edge-down k8s-status \
	cluster-create cluster-delete ingress-install ingress-wait \
	skaffold-dev skaffold-run skaffold-delete

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*##"; printf "\nAvailable targets:\n"} /^[a-zA-Z0-9_.-%-]+:.*##/ {printf "  %-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: $(addprefix build-,$(SERVICES)) ## Build all services

test: $(addprefix test-,$(SERVICES)) ## Test all services

clean: $(addprefix clean-,$(SERVICES)) ## Clean all services

build-%: ## Build one service (config|catalog|order|edge)
	cd $(SERVICE_DIR_$*) && ./gradlew clean build

test-%: ## Test one service (config|catalog|order|edge)
	cd $(SERVICE_DIR_$*) && ./gradlew test

clean-%: ## Clean one service (config|catalog|order|edge)
	cd $(SERVICE_DIR_$*) && ./gradlew clean

run-%: ## Run one service locally (config|catalog|order|edge)
	cd $(SERVICE_DIR_$*) && ./gradlew bootRun

cluster-create: ## Create kind cluster and install ingress-nginx
	kind create cluster --name $(KIND_CLUSTER) --config $(KIND_CONFIG)
	$(MAKE) ingress-install

cluster-delete: ## Delete kind cluster
	kind delete cluster --name $(KIND_CLUSTER)

ingress-install: ## Install ingress-nginx in current cluster
	kubectl apply --context $(KUBE_CONTEXT) -f $(INGRESS_NGINX_MANIFEST)
	$(MAKE) ingress-wait

ingress-wait: ## Wait for ingress-nginx controller to be ready
	kubectl wait --context $(KUBE_CONTEXT) \
		--namespace ingress-nginx \
		--for=condition=ready pod \
		--selector=app.kubernetes.io/component=controller \
		--timeout=180s

platform-up: ## Apply local backing services (Postgres + Redis)
	kubectl apply --context $(KUBE_CONTEXT) -f $(K8S_PLATFORM_FILES)

platform-down: ## Delete local backing services (Postgres + Redis)
	kubectl delete --context $(KUBE_CONTEXT) -f $(K8S_PLATFORM_FILES)

edge-up: ## Apply edge-service Kubernetes manifests (Deployment/Service/Ingress)
	kubectl apply --context $(KUBE_CONTEXT) -f $(EDGE_K8S_FILES)

edge-down: ## Delete edge-service Kubernetes manifests
	kubectl delete --context $(KUBE_CONTEXT) -f $(EDGE_K8S_FILES)

k8s-status: ## Show deployments, services, ingress, and pods
	kubectl get --context $(KUBE_CONTEXT) deployments,services,ingress,pods

skaffold-dev: ## Run Skaffold dev against kind cluster
	skaffold dev -f $(SKAFFOLD_FILE) -p kind

skaffold-run: ## Run Skaffold once against kind cluster
	skaffold run -f $(SKAFFOLD_FILE) -p kind

skaffold-delete: ## Delete resources managed by Skaffold
	skaffold delete -f $(SKAFFOLD_FILE) -p kind
