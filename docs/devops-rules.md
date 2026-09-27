# devops-rules.md — Environments, Build, Config & Operations

> Owner: DevOps / Architect · Status: **Baseline v1.0**

## 1. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java | 17+ | backend |
| Node | 18+ (24 tested) | frontend |
| Maven | via `./mvnw` | no global install needed |
| MySQL | 8.x | profile `mysql` |
| H2 | bundled | profile `dev` (no DB install) |
| Docker (optional) | recent | compose lab + UC-14 reset |

## 2. Environments & profiles

| Profile | DB | DDL | Secrets | Use |
|---|---|---|---|---|
| `dev` (default) | H2 in-file/mem, MySQL mode | `ddl-auto=update` | env or defaults | local run without MySQL |
| `mysql` | MySQL 8 | `ddl-auto=update` (first run) → `validate` | **env required** | primary target |
| `test` | H2 mem | `create-drop` | test fixtures | CI/tests |

## 3. Configuration & secrets (BR-10)

- Non-secret config in `application.yml` / `application-<profile>.properties`.
- Secrets **only** via environment variables:
  `DB_PASSWORD`, `AUTHSHIELD_SIGNING_KEY`, `SMTP_PASSWORD`, `AUTHSHIELD_SMTP_*`.
- `.env`, `*.local.properties`, `uploads/` are git-ignored.
- **Never** commit a real secret. Placeholders in `.env.example` only.
- Startup fails fast if `mysql` profile is active and a required secret is missing.

## 4. Build commands

```bash
# backend
cd backend
./mvnw -q clean verify                      # build + tests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql

# frontend
cd frontend
npm ci
npm run dev                                 # http://localhost:5173 (proxy /api -> :8080)
npm run build                               # production bundle
```

## 5. Database lifecycle

```bash
# MySQL (profile mysql)
mysql -u root -p < db/schema.sql
mysql -u root -p authshield360 < db/seed.sql
```

**Reset (UC-14, 30–60 s):**
```bash
# docker path
docker compose down -v && docker compose up -d
# non-docker path
mysql -u root -p authshield360 < db/reset.sql   # truncate + reseed
```
Before reset: back up `audit_logs` if evidence must be preserved.

**Persistence check (BR-12):** restart the service → accounts, roles, MFA config and audit log must
still be present.

## 6. Repository layout

```
/
├── backend/     Spring Boot app (+ mvnw wrapper)
├── frontend/    React + Vite app
├── db/          schema.sql, seed.sql, reset.sql
├── docs/        12 governance documents
├── docker-compose.yml
├── .env.example
└── README.md
```

## 7. CI outline (quality gates)

```
[checkout] → [backend: ./mvnw verify] → [frontend: npm ci && npm run build && npm run lint]
          → [secret-scan] → [docker build] → [publish artifacts]
```
Gates: build green, tests green, lint clean, no secret detected, docs present.

## 8. Runtime & observability

- Structured logs (SLF4J), `X-Correlation-Id` per request.
- `/actuator/health` (if enabled) for liveness; audit is the business observability layer (BR-06).
- Upload directory outside the repo (`uploads/`), size/type validated.

## 9. Backup & retention

- Nightly `mysqldump` of `authshield360` (lab schedule).
- Audit retention policy: keep ≥ project duration; partition monthly at scale; export cap 10,000 rows.

## 10. Rollback

Additive, reversible changes preferred. Schema changes ship with a down-path or backup+restore.
Application rollback = redeploy previous artifact; config rollback = previous `auth_config` export (UC-08 B5).

## 11. DoD (ops)

Runs locally with `dev` in < 2 min; `mysql` run documented and verified; reset ≤ 60 s; secrets
externalised; README reproducible by a new team member without tribal knowledge.
