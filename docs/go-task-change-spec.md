# go-task-change-spec.md — Change Specification & Task Template

> Owner: Technical Lead (Change Control) · Status: **Baseline v1.0**
> Purpose: every change ("go-task") enters through this template so BA, Dev, QA, Security and Ops
> share one view of intent, impact and acceptance.

## 1. When to use

Use for **any** change to code, schema, config, or behaviour after M1 baseline — including a new
use case, a rule change, a bug fix with side effects, a dependency bump, or an infra change.

## 2. Change request template (copy into the PR description)

```md
## GO-TASK <ID> — <short title>
- Type: feature | bugfix | refactor | perf | security | docs | ops
- Owner: <name>            - Reviewer(s): <names>
- Requested by: <stakeholder>
- Traceability: UC-__ / FR-__ / BR-__ / TC-__

### 1. Context
Why is this change needed? What problem/defect does it solve?

### 2. Scope
IN scope: ...
OUT of scope: ...

### 3. Functional spec delta
Describe behaviour before → after. Include UI element IDs affected.
List any new/changed messages (use ba-rules §4 keys).

### 4. Data impact
New/changed tables, columns, indexes, migrations, backfill, retention.

### 5. API impact
Endpoints added/changed, request/response shape, status codes, backwards compatibility.

### 6. Security & RBAC (BR-05, security-bac.md)
Roles affected. New permissions. Threat considerations. Secrets touched (BR-10)?
Abuse cases: brute force, enumeration, replay, privilege escalation.

### 7. Audit & observability (BR-06/07)
New events (ba-rules §5) and their fields. Correlation id. Log sanitisation.

### 8. Acceptance criteria
- [ ] Given ... when ... then ...
- [ ] ... (each must be testable)

### 9. Test plan
New/updated TCs in qa-rules.md. Negative cases. Regression scope.

### 10. Rollout & rollback
Config flags, migration steps, rollback steps, reset impact (UC-14).

### 11. Risks
Likelihood × impact, mitigation.

### 12. Definition of Done checklist
Code ✓ / Tests ✓ / Audit ✓ / RBAC ✓ / Docs ✓ / Secret-scan ✓ / FE responsive+IDs ✓
```

## 3. Approval gates

| Gate | Approver | Checks |
|---|---|---|
| G1 Intent | BA | matches UC/BR, message catalogue updated |
| G2 Design | Architect | architecture/database/API impact, ADR if needed |
| G3 Security | Security owner | `security-bac.md` threat review, no secret leak |
| G4 QA | QA lead | test cases + evidence plan |
| G5 Release | Tech lead | DoD complete, rollback proven |

## 4. Severity & priority

| Severity | Meaning | SLA |
|---|---|---|
| S1 Critical | auth bypass, data loss, secret leak | immediate |
| S2 High | core use case broken, RBAC gap | 1 day |
| S3 Medium | degraded UX, partial feature | 1 sprint |
| S4 Low | cosmetic/docs | backlog |

## 5. Change classification → required docs

| Change type | Must update |
|---|---|
| New UC / rule | `use-cases.md`, `ba-rules.md`, `qa-rules.md` |
| Schema | `database.md`, migration, `architecture.md` §11 ADR |
| API | `architecture.md` §3, `be-rules.md` §3, `codegraph.md` |
| UI | `fe-rules.md`, mandatory-ID list, `qa-rules.md` |
| Security | `security-bac.md`, `be-rules.md` §4 |
| Infra | `devops-rules.md`, compose/env |

## 6. Example — GO-TASK-014 (reference implementation)

```
GO-TASK-014 — Allow students to resubmit a submitted file while window open
Type: feature      Traceability: UC-A4, BR-05, TC-A4
Context: Students can submit but cannot correct an upload before the deadline.
Scope IN: resubmission while PUBLISHED and allow_resubmission and attempts<max.
Scope OUT: editing after CLOSED/grade.
Functional: new attempt row; latest attempt is authoritative; history retained.
Data: assignment_submissions already supports attempt_number (no migration).
API: POST /api/v1/assignments/{id}/submissions (upsert-new-attempt).
Security: STUDENT role only; ownership check server-side; 409 when locked.
Audit: SUBMISSION_CREATE / SUBMISSION_UPDATE / SUBMISSION_BLOCKED.
Acceptance: resubmit while open → 201 new attempt; after close → 409 SUBMISSION_LOCKED.
Tests: TC-A4a success, TC-A4b max attempts, TC-A4c after close, TC-A3 locked.
Rollout: no flag; rollback = revert endpoint commit.
```
