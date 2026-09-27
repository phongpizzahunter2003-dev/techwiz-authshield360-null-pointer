# delivery.md — Delivery Plan, Milestones & Deliverables

> Owner: Technical Lead / PM · Status: **Baseline v1.0**

## 1. Deliverable inventory (what the SRS/task asks for)

| # | Deliverable | Location | Status target |
|---|---|---|---|
| D1 | Frontend SPA (React), 3 role dashboards | `frontend/` | Working |
| D2 | Backend API (Spring Boot) + MySQL | `backend/` | Working |
| D3 | Database schema + seed + reset | `db/` | Working |
| D4 | 12 governance docs | `docs/` | Complete |
| D5 | Auth modes S1/S2/S3 | `backend/.../auth` | Working |
| D6 | RBAC + access matrix | code + docs | Working |
| D7 | Audit log + viewer + masked export | code | Working |
| D8 | Assignment cases A1–A4 | code + docs | Working |
| D9 | Test cases TC-01..TC-12 + A-cases | `docs/qa-rules.md` | Complete |
| D10 | Reproducible run (compose + README) | root | Working |

## 2. Milestones

| M | Name | Exit criteria |
|---|---|---|
| M0 | Governance docs | 12 docs approved/committed |
| M1 | Foundation | backend compiles; FE builds; DB schema applies |
| M2 | Auth engine | S1/S2/S3 end-to-end; lockout; resend-throttle; sessions |
| M3 | RBAC + audit | 3 dashboards; 403 path; log viewer + export |
| M4 | School domain | assignments + submissions A1–A4; grading; exam results |
| M5 | Hardening | validation, error envelope, secrets, tests |
| M6 | Delivery | README, seed, compose, evidence pack |

## 3. Work breakdown (module-level)

| Module | Backend | Frontend |
|---|---|---|
| Auth | auth controller/service, otp, lockout, sessions | login wizard, countdown, resend, lockout |
| Users/Roles | admin user CRUD, role assign | admin users page |
| Config | auth_config service | config page (mode, OTP, SMTP) |
| Audit | audit service, export, filters | log viewer, export, filters |
| School | classrooms, assignments, submissions, results | student/teacher pages |
| Dashboards | per-role aggregations | 3 dashboards |
| Comparison | S1/S2/S3 metrics (UC-15) | comparison report |

## 4. Branching & commits

- Trunk-based: `main` protected; work on `feat/<scope>`, `fix/<scope>`, `docs/<scope>`.
- Commits: Conventional Commits (`feat(auth): ...`), small and focused.
- Every PR references the UC/TC id(s) it satisfies.

## 5. Definition of Ready / Done

**Ready:** UC id, acceptance criteria, affected modules, test cases, RBAC impact, audit events.
**Done:** code + tests green; UC flows (incl. alt/exception) work; audit emitted; RBAC enforced;
docs updated; evidence attached; no secrets; demonstrable in ≤ 60 s reset (UC-14).

## 6. Evidence pack (for judges)

`evidence/` folder gathering: screenshots per TC, exported masked logs (max 10k),
S1/S2/S3 timing results (≥ 3 runs each, BR-09), reset duration, and the filled 7-column matrix.

## 7. Timeline (indicative, sprint = 1 week)

| Sprint | Focus | Milestones |
|---|---|---|
| 0 | Docs + scaffold | M0, M1 |
| 1 | Auth engine | M2 |
| 2 | RBAC, audit, dashboards | M3 |
| 3 | School domain | M4 |
| 4 | Hardening + delivery | M5, M6 |

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| MySQL/smtp not available locally | H2 `dev` profile + simulated mail (VĐ-06) |
| Email OTP scope ambiguity (VĐ-01) | Implement fully; document assumption |
| OTP brute-force | VĐ-04: wrong OTP counts toward lockout + rate limit |
| Secret leakage (BR-10) | env-only + gitignore + secret scan in QA |
| Reset loses audit (UC-14) | backup before reset; verify persistence (BR-12) |
