SHELL := /bin/bash
COMPOSE ?= docker compose
MVN ?= ./mvnw
NPM ?= npm

.PHONY: help up down logs backend frontend install test backend-test frontend-test smoke db-reset full-up full-down clean

help:
	@echo "TaskFlow — comandos"
	@echo ""
	@echo "  make up            Postgres + PostgREST en Docker"
	@echo "  make backend       Spring Boot en :8080 (usa ./mvnw)"
	@echo "  make frontend      Vite en :5173"
	@echo "  make test          Tests backend + typecheck/build frontend"
	@echo "  make smoke         Prueba real de la cadena API -> PostgREST -> DB"
	@echo "  make db-reset      Borra el volumen y re-siembra (destructivo)"
	@echo "  make full-up       Stack completo en contenedores (:8081)"
	@echo "  make down          Apaga los contenedores"
	@echo ""

up:
	$(COMPOSE) up -d db postgrest

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f --tail=100

backend:
	cd backend && $(MVN) spring-boot:run

frontend:
	cd frontend && $(NPM) run dev

install:
	cd frontend && $(NPM) ci

test: backend-test frontend-test

backend-test:
	cd backend && $(MVN) test

frontend-test:
	cd frontend && $(NPM) run typecheck && $(NPM) run build

smoke:
	./scripts/smoke.sh

db-reset:
	@read -p "Esto borra la base local. ¿Continuar? [y/N] " ok; [[ $$ok =~ ^[yY]$$ ]] || exit 1
	$(COMPOSE) down -v
	$(COMPOSE) up -d db postgrest

full-up:
	$(COMPOSE) -f compose.full.yaml up --build -d

full-down:
	$(COMPOSE) -f compose.full.yaml down

clean:
	cd backend && $(MVN) clean
	rm -rf frontend/dist
