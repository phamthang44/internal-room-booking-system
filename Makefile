# Makefile — Docker environment shortcuts (Git Bash / WSL / Linux / Mac)
# ─────────────────────────────────────────────────────────────────────────────
# Analogous to Maven Profiles:
#   make infra   → infra only, run Spring from IDE (recommended daily workflow)
#   make dev     → full dev stack in Docker
#   make prod    → like mvn -Pprod (production)
#
# On Windows: use Git Bash or WSL, or use run.ps1 in PowerShell instead.
# ─────────────────────────────────────────────────────────────────────────────

BASE := docker-compose.base.yml
DEV  := docker-compose.dev.yml
PROD := docker-compose.prod.yml

.PHONY: infra dev prod dev-down prod-down dev-logs prod-logs dev-build prod-build prod-pull prod-push dev-restart prod-restart ps help

## ── RECOMMENDED DAILY WORKFLOW ───────────────────────────────────────────────
## Start infra only, then run Spring from IntelliJ with profile=dev
infra:
	@echo "🚀 Starting dev infra (postgres, redis, rabbitmq, maildev)..."
	@echo "   MailDev inbox : http://localhost:1080"
	@echo "   RabbitMQ UI   : http://localhost:15672  (guest / guest)"
	@echo "   Postgres      : localhost:5432"
	@echo "   Redis         : localhost:6379"
	@echo ""
	@echo "   → Run Spring from IntelliJ with profile=dev"
	docker compose -f $(BASE) -f $(DEV) up -d postgres redis rabbitmq maildev

## ── FULL DEV STACK (all services in Docker) ──────────────────────────────────
dev:
	@echo "🚀 Starting full dev stack..."
	@echo "   App      : http://localhost:8080"
	@echo "   MailDev  : http://localhost:1080"
	@echo "   RabbitMQ : http://localhost:15672"
	docker compose -f $(BASE) -f $(DEV) up -d

dev-down:
	docker compose -f $(BASE) -f $(DEV) down

dev-logs:
	docker compose -f $(BASE) -f $(DEV) logs -f backend-app

dev-build:
	docker compose -f $(BASE) -f $(DEV) build

dev-restart:
	docker compose -f $(BASE) -f $(DEV) restart backend-app

## ── PRODUCTION ───────────────────────────────────────────────────────────────
prod:
	@echo "🚀 Starting production stack..."
	docker compose -f $(BASE) -f $(PROD) up -d

prod-down:
	docker compose -f $(BASE) -f $(PROD) down

prod-logs:
	docker compose -f $(BASE) -f $(PROD) logs -f backend-app

prod-build:
	docker compose -f $(BASE) -f $(PROD) build

prod-restart:
	docker compose -f $(BASE) -f $(PROD) up -d

prod-pull:
	docker compose -f $(BASE) -f $(PROD) pull

prod-push:
	docker compose -f $(BASE) -f $(PROD) push

## ── UTILITIES ────────────────────────────────────────────────────────────────
ps:
	@echo "=== DEV ===" && docker compose -f $(BASE) -f $(DEV) ps 2>/dev/null || true
	@echo "=== PROD ==="&& docker compose -f $(BASE) -f $(PROD) ps 2>/dev/null || true

help:
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "  infra         Start infra only (postgres, redis, rabbitmq, maildev)"
	@echo "  dev           Full dev stack in Docker"
	@echo "  dev-down      Stop dev stack"
	@echo "  dev-logs      Follow backend-app logs (dev)"
	@echo "  dev-build     Rebuild dev images"
	@echo "  prod          Production stack"
	@echo "  prod-down     Stop prod stack"
	@echo "  prod-logs     Follow backend-app logs (prod)"
	@echo "  prod-build    Rebuild prod images"
	@echo "  prod-pull     Pull latest images for prod"
	@echo "  prod-push     Push latest images to Docker Hub"
	@echo "  ps            Show running containers for both envs"
