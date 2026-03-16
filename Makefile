SHELL := /bin/bash

COMPOSE_FILE := polar-deployment/docker/docker-compose.yml
K8S_DIR := polar-deployment/kubernetes/local
K8S_FILES := $(K8S_DIR)/postgresql.yml $(K8S_DIR)/postgresql-order.yml
SKAFFOLD_FILE := skaffold.yml

.PHONY: help build test clean format lint \
	build-config build-catalog build-order \
	test-config test-catalog test-order \
	clean-config clean-catalog clean-order \
	run-config run-catalog run-order \
	infra-up infra-down compose-up compose-down \
	k8s-up k8s-down k8s-status \
	skaffold-dev skaffold-run skaffold-delete

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*##"; printf "\nAvailable targets:\n"} /^[a-zA-Z0-9_.-]+:.*##/ {printf "  %-16s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: build-config build-catalog build-order ## Build all services

test: test-config test-catalog test-order ## Test all services

clean: clean-config clean-catalog clean-order ## Clean all services

format: ## Run Spotless auto-format (catalog + order)
	cd catalog-service && ./gradlew spotlessApply
	cd order-service && ./gradlew spotlessApply

lint: ## Run Spotless checks (catalog + order)
	cd catalog-service && ./gradlew spotlessCheck
	cd order-service && ./gradlew spotlessCheck

build-config: ## Build config-service
	cd config-service && ./gradlew clean build

build-catalog: ## Build catalog-service
	cd catalog-service && ./gradlew clean build

build-order: ## Build order-service
	cd order-service && ./gradlew clean build

test-config: ## Run tests for config-service
	cd config-service && ./gradlew test

test-catalog: ## Run tests for catalog-service
	cd catalog-service && ./gradlew test

test-order: ## Run tests for order-service
	cd order-service && ./gradlew test

clean-config: ## Clean config-service outputs
	cd config-service && ./gradlew clean

clean-catalog: ## Clean catalog-service outputs
	cd catalog-service && ./gradlew clean

clean-order: ## Clean order-service outputs
	cd order-service && ./gradlew clean

run-config: ## Run config-service locally
	cd config-service && ./gradlew bootRun

run-catalog: ## Run catalog-service locally
	cd catalog-service && ./gradlew bootRun

run-order: ## Run order-service locally
	cd order-service && ./gradlew bootRun

infra-up: ## Start local PostgreSQL containers
	docker compose -f $(COMPOSE_FILE) up -d polar-postgres-catalog polar-postgres-order

infra-down: ## Stop local PostgreSQL containers
	docker compose -f $(COMPOSE_FILE) stop polar-postgres-catalog polar-postgres-order

compose-up: ## Start full Docker Compose stack
	docker compose -f $(COMPOSE_FILE) up -d

compose-down: ## Stop and remove Docker Compose stack
	docker compose -f $(COMPOSE_FILE) down

k8s-up: ## Apply local Kubernetes PostgreSQL manifests
	kubectl apply -f $(K8S_FILES)

k8s-down: ## Delete local Kubernetes PostgreSQL manifests
	kubectl delete -f $(K8S_FILES)

k8s-status: ## Show Kubernetes resources in current namespace
	kubectl get deployments,services,pods

skaffold-dev: ## Run Skaffold in dev mode (requires skaffold.yml)
	@if [ ! -f $(SKAFFOLD_FILE) ]; then \
		echo "Missing $(SKAFFOLD_FILE). Add it or set SKAFFOLD_FILE=<path>."; \
		exit 1; \
	fi
	skaffold dev -f $(SKAFFOLD_FILE)

skaffold-run: ## Run Skaffold once (build/deploy)
	@if [ ! -f $(SKAFFOLD_FILE) ]; then \
		echo "Missing $(SKAFFOLD_FILE). Add it or set SKAFFOLD_FILE=<path>."; \
		exit 1; \
	fi
	skaffold run -f $(SKAFFOLD_FILE)

skaffold-delete: ## Remove resources managed by Skaffold
	@if [ ! -f $(SKAFFOLD_FILE) ]; then \
		echo "Missing $(SKAFFOLD_FILE). Add it or set SKAFFOLD_FILE=<path>."; \
		exit 1; \
	fi
	skaffold delete -f $(SKAFFOLD_FILE)
