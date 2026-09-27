# AuthShield 360 — School Portal (AuthenticatorShield360)

A simulated school portal protected by a configurable, multi-layer authentication platform. It is
used to **compare three authentication modes** (S1 / S2 / S3), exercise **RBAC**, **audit logging**,
**account lockout**, **in-app notifications** and the student assignment workflows.

> ⚠️ **Lab use only**, with simulated accounts and data (BR-01). Never use real student data.

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite + Tailwind CSS + React Router + Axios + Recharts |
| Backend | Spring Boot 4.1 (Java 17) + Spring Security + Spring Data JPA |
| Database | MySQL 8 (primary) · H2 (profile `dev`, runs with no database install) |
| Security | BCrypt (cost 12), HMAC tokens, server-side session registry, AES-GCM secret storage, server-enforced RBAC |


## 1. Repository layout

```
authshieldtest/
├── backend/        Spring Boot API (Maven Wrapper included — no Maven install needed)
├── frontend/       React SPA (Vite)
├── db/             schema.sql · seed.sql · reset.sql
├── docs/           12 governance documents (architecture, BA rules, DB, FE/BE rules, ...)
├── docker-compose.yml
└── .env.example
```

---

## 2. Quick start (dev, no MySQL required)

Requirements: **Java 17+**, **Node 18+**.

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Windows: .\mvnw.cmd ...
```
- API: http://localhost:8080
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:authshield360`, user `sa`)
- Demo data is seeded automatically on first start (see section 4).

### Frontend

```bash
cd frontend
npm install
npm run dev
```
- SPA: http://localhost:5173 (Vite proxies `/api` → `http://localhost:8080`)

---

## 3. Running with MySQL (primary target)

The backend connects with the MySQL driver. Two servers can be used — pick whichever is running:

| Option | Typical JDBC | Notes |
|---|---|---|
| **MySQL Server 8** (installed as service `MySQL80`) | `jdbc:mysql://localhost:3306/authshield360` | No dialect override needed |
| **MariaDB** (e.g. the server bundled with XAMPP) | same URL, port 3306 | Reports version `5.5.5-...-MariaDB`, so set `DB_DIALECT` to the MariaDB dialect |

```powershell
# 1) Create the schema and default configuration (14 tables)
#    XAMPP example — use your own mysql.exe path if MySQL Server 8 is installed:
& 'C:\xampp\mysql\bin\mysql.exe' -u root --default-character-set=utf8mb4 -e "source <repo>/db/schema.sql"
& 'C:\xampp\mysql\bin\mysql.exe' -u root authshield360 --default-character-set=utf8mb4 -e "source <repo>/db/seed.sql"

# 2) Provide the connection settings and a stable signing key (never committed — BR-10)
$env:DB_HOST='127.0.0.1'; $env:DB_PORT='3306'
$env:DB_NAME='authshield360'; $env:DB_USER='root'; $env:DB_PASSWORD=''
$env:AUTHSHIELD_SIGNING_KEY='<random 32+ characters>'
# MariaDB only (XAMPP):
$env:DB_DIALECT='org.hibernate.dialect.MariaDBDialect'
# Optional, for demos where no real SMS/e-mail is delivered:
$env:AUTHSHIELD_EXPOSE_OTP='true'

# 3) Run
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

On first start the seeder creates the four demo accounts; on every later start the existing data
is reused (**BR-12**: accounts, roles, MFA configuration and the audit log survive a restart).

### Where the data lives and how to inspect it

- **On disk**: the server's data directory — e.g. `C:\xampp\mysql\data\authshield360\` (one
  `.ibd`/`.frm` pair per table).
- **In the app**: *Admin → System & data* (`/admin/system`) shows the product, JDBC URL,
  `persistent: true` and the row count of every table.
- **SQL client**: `mysql -u root authshield360 -e "SELECT id, username, role FROM users;"`
- **phpMyAdmin** (XAMPP): http://localhost/phpmyadmin → database `authshield360`
- **MySQL Workbench**: connect to `localhost:3306`, schema `authshield360`

Fast reset (UC-14): apply `db/reset.sql` or `docker compose down -v && docker compose up -d`.

---

## 4. Demo accounts and seeded sample data (simulated — BR-01)

The seeder creates **1 admin + 20 teachers + 20 students (20 sample records per group)** plus
classrooms, assignments, submissions, exam results and notifications, so every list, chart and
pagination control has realistic content.

| Group | Accounts | Password |
|---|---|---|
| Administrator | `admin01` | `admin123` |
| Teachers (20) | `teacher01` … `teacher20` | `teacher123` |
| Students (20) | `student01` … `student20` | `student123` |

Seeded volume on a fresh database (measured):

| Table | Rows |
|---|---|
| users | 45 (1 admin · 20 teachers · 20 students · 4 test fixtures) |
| classrooms | 20 (CS101 … CS120, one owner teacher each) |
| enrollments | 41 (each student in 2 classes; CS101 keeps student01 + student02; `student_lonely01` in none) |
| assignments | 46 (8 scenario assignments in CS101, one per submission rule; 2 for each other class) |
| assignment_submissions | 67 (6 hand-written scenario rows + auto-generated mix of on-time / late / graded) |
| exam_results | 40 (2 per student) |
| notifications | 245 (welcome + graded + submission-received) |

Convenient accounts for demonstrations:

- `student01` — the flagship class CS101, exercises the scenario assignments (on-time, late, locked, resubmit).
- `teacher01` — owns CS101 (the class used in all the tutorial screenshots).
- `teacher05`, `student10` — ordinary seeded accounts with submissions/grades already present.

### 4.1 Test-fixture accounts (one per hard-to-reach scenario)

These extra accounts are pre-set to a specific state so every case in
[`docs/test-cases.md`](docs/test-cases.md) has its data ready. Password: `student123`.

| Account | Pre-set state | Used by |
|---|---|---|
| `locked01` | `LOCKED`, 5 failed attempts, locked for 15 minutes | AUTH-05 (login while locked) |
| `disabled01` | `DISABLED` | AUTH-08 (disabled account) |
| `student_mfa01` | MFA **enrolled** with the known demo TOTP secret `JBSWY3DPEHPK3PXP` | AUTH-26 (real authenticator app) |
| `student_lonely01` | active but **enrolled in no class** | STU-19 (empty states) |
| `student03` | enrolled in CS103/CS104, **not** in CS101 | STU-06 / TEA-20 (not-enrolled cases) |

### 4.2 CS101 scenario assignments (one per submission rule)

| Assignment | Rule it demonstrates |
|---|---|
| Assignment 1 - Loops and arrays | submit on time (student02 already submitted; student01 free) |
| Assignment 2 - Recursion (late allowed) | late submission accepted |
| Assignment 3 - Data structures (no late) | late submission refused |
| Assignment 4 - Resubmission allowed | update the submitted file (student01 has attempt 1, student02 graded) |
| Assignment 5 - Late window closed | late window already closed |
| Assignment 6 - Single attempt only | attempt limit reached (student02) |
| Assignment 7 - Resubmission not allowed | resubmission refused (student02 submitted) |
| Midterm exam (closed) | assignment closed — no submit/update |

The system starts in **S1 (password only)** so you can sign in immediately. Switch to **S2** (adds
Mobile OTP) or **S3** (adds Email OTP) in **Admin → Auth configuration**.


---

## 5. Authentication modes

| Mode | Flow | Factors |
|---|---|---|
| **S1** | Password → session | `PASSWORD` |
| **S2** | Password → Mobile OTP → session | `PASSWORD`, `MOBILE_OTP` |
| **S3** | Password → Mobile OTP → Email OTP → session | `PASSWORD`, `MOBILE_OTP`, `EMAIL_OTP` |

Mobile OTP supports **TOTP** (authenticator app, QR shown on the profile page) or a **simulated SMS**
channel. Email OTP uses the SMTP configuration from the admin console (simulated by default).

Each mode has its own detail page at `/admin/modes/S1|S2|S3` describing its flow, the measured
metrics, and which configuration parameters actually apply.

---

## 6. Role dashboards

- **Student** (`/student`): overview, assignment list, submit/resubmit, submission history, exam results.
- **Teacher** (`/teacher`): classes, assignment create/edit/close, grading, class rosters.
- **Administrator** (`/admin`): users & roles, auth configuration, audit log + export, S1/S2/S3
  comparison, system & data diagnostics.

### Charts (role-specific logic)

| Role | Charts | Click-through |
|---|---|---|
| Student | PIE assignment status; BAR submissions per assignment; BAR score per exam | filtered assignment list (`?bucket=`), assignment detail, exam results |
| Teacher | BAR submissions per assignment; PIE grading progress; BAR students per class | assignment detail (grading), class detail (roster + results) |
| Admin | BAR sign-ins by mode S1/S2/S3; PIE users by role; BAR security events; LINE events over 7 days | mode detail page, filtered audit log (`?mode=`/`?action=`/`?from=`/`?to=`), users by role |

Charts read live from `/api/v1/analytics/{student|teacher|admin}` (SQL aggregation), so they update
as soon as data changes. **Everything displayed is clickable** — stat cards, chart elements, legend
chips and table rows all lead to an authorised detail page, and every detail page has a **Back** button.

---

## 7. Notifications

Students and teachers receive **in-app notifications** with read/unread state:

- Bell in the top bar with an unread badge; dropdown lists the latest 6; `/notifications` shows the
  full list with **Unread / All** filters, per-item **Mark read** / **Dismiss**, and **Mark all as read**.
- Generated by domain events: assignment published / updated / closed, submission received
  (on time vs late), submission graded, class enrolment, and a welcome message.

| Event | Recipient | Type |
|---|---|---|
| Teacher publishes an assignment | every student in the class | `ASSIGNMENT_PUBLISHED` |
| Teacher edits an assignment | students | `ASSIGNMENT_UPDATED` |
| Teacher closes an assignment | students | `ASSIGNMENT_CLOSED` |
| Student submits | the owning teacher | `SUBMISSION_RECEIVED` / `SUBMISSION_LATE` |
| Teacher grades | the student | `SUBMISSION_GRADED` |
| Student enrolled in a class | the student | `CLASSROOM_ENROLLED` |

API: `GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`,
`POST /api/v1/notifications/{id}/read`, `POST /api/v1/notifications/read-all`,
`DELETE /api/v1/notifications/{id}` (ownership enforced server-side).

---

## 8. Who enrols MFA? (UC-08 / UC-09 / UC-16)

**Every portal user enrols MFA for themselves** (student, teacher or administrator) by scanning the
QR on **Profile & MFA**. Per UC-09, after the first successful password sign-in the portal prompts the
user to enrol when MFA is enabled but not yet registered.

Administrators **do not create a user's QR**: they configure the authentication mode (UC-08), can
**assign S1/S2/S3 per user or in bulk** (Users & roles), and can **reset** a user's MFA (UC-16) so the
user re-enrols.

---

## 9. Student assignment scenarios (project extension)

| Code | Scenario | Result |
|---|---|---|
| **UC-A1** | Submit on time | stored as `ON_TIME`, attempt #1 |
| **UC-A2** | Submit late | `LATE` when `allowLate`; `409 LATE_NOT_ALLOWED` after the cutoff or when late is disabled |
| **UC-A3** | Cannot update homework | `409 SUBMISSION_LOCKED` / `RESUBMISSION_NOT_ALLOWED` / `MAX_ATTEMPTS_REACHED` + `SUBMISSION_BLOCKED` audit |
| **UC-A4** | Submit and update the file | new attempt (`attemptNumber + 1`), history retained, newest is authoritative |

---

## 10. Governance documents (`docs/`)

| File | Contents |
|---|---|
| `architecture.md` | Architecture, S1/S2/S3 flows, sessions/tokens, ADRs |
| `ba-rules.md` | BR-01..BR-12, decisions VD-01..VD-08 and C-01..C-06, message + audit catalogues |
| `database.md` | Data model, DDL, constraints |
| `fe-rules.md` | Frontend rules, design tokens, mandatory element IDs, chart/back-button contract |
| `be-rules.md` | Backend rules, security, validation, transactions |
| `delivery.md` | Plan, milestones, deliverables, DoD |
| `go-task-change-spec.md` | Change-control template and example |
| `security-bac.md` | Broken-access-control test matrix, threat model |
| `codegraph.md` | Module graph, call graphs, ownership |
| `devops-rules.md` | Environments, build, secrets, reset, CI |
| `qa-rules.md` | Test strategy, TC-01..TC-12, TC-A1..A4, TC-D*, TC-M*, TC-P*, evidence rules |
| `use-cases.md` | Detailed use cases and FR ↔ UC traceability |



---

## 11. Testing

```bash
cd backend && ./mvnw verify            # unit + context tests
cd frontend && npm run build           # production build (also validates every import)
```

See `docs/qa-rules.md` for the mandatory test set and the evidence rules (7-column matrix:
Test ID · User/Role · Action · Expected · Actual · Pass/Fail · Evidence).

---

## 12. Security highlights

- Passwords hashed with **BCrypt cost 12** (BR-02); OTP codes stored hashed; MFA and SMTP secrets
  encrypted with **AES-GCM** (BR-10).
- **RBAC enforced server-side** on every endpoint (`@PreAuthorize` plus URL rules) — the client is never trusted.
- **Lockout** after 5 failed attempts, escalating 1→5→15 minutes; **wrong OTP counts** toward the threshold (VD-04).
- **OTP resend**: server-side 60-second throttle → HTTP 429; maximum 3 resends then restart (VD-07).
- **Server-side session registry**: sign-out, expiry and replay are all rejected (BR-08).
- Complete audit log with PII masking on export and a 10,000-row export cap.
- Destructive actions (delete user / delete class) require an explicit confirmation dialog.

See `docs/security-bac.md` for details.

---

## 13. Troubleshooting

| Symptom | Fix |
|---|---|
| Port 8080 already in use | Change `server.port` or stop the process using the port |
| Cannot sign in | Check the backend is running and read the audit log; the account may be temporarily locked |
| No OTP received | In `dev` the code appears in the UI and the `[SIMULATED-SMS]` log line; for S3 make sure Email OTP is enabled |
| QR scan says "only open with an app" | Open the QR scanner **inside** the authenticator app, or use manual key entry and paste the secret |
| MySQL connection error | Check `DB_*` in `.env` and make sure the schema exists |
| `mvn` not found | Use the bundled wrapper: `./mvnw` |
