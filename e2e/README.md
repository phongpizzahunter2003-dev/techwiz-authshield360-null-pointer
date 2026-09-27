# AuthShield 360 — browser automation (Playwright)

End-to-end UI tests for the automation cases in [`../docs/test-cases.md`](../docs/test-cases.md).
They drive a real Chromium browser against the running application: the login wizard (S1/S2/S3),
locked/disabled accounts, captcha, the TOTP flow, RBAC, the top-of-screen alert layer, the student
submit/update UX, teacher class/assignment/grading flows and the administrator pages.

> The API-level cases (lockout thresholds, OTP expiry, resend limits, submission rules, export
> masking, audit fields…) are covered by the JUnit suite in `backend/src/test/java/.../automation`.
> This suite focuses on what only a browser can prove: rendering, navigation, disabled buttons and
> alerts.

## Prerequisites

1. **Backend** running with the `dev` profile (or `mysql`), on `http://localhost:8080`.
   The `dev` profile uses in-memory H2 and re-seeds the lab fixtures on every start, which makes
   a run deterministic and exposes OTP codes in the UI (`authshield.expose-otp=true`).
2. **Node.js 18+**.
3. Playwright's Chromium: `npm install` then `npx playwright install chromium`.

The Vite dev server is started automatically by Playwright (`reuseExistingServer: true`), so an
already-running `npm run dev` is reused.

## Run it

```bat
cd e2e
npm install
npx playwright install chromium

:: easiest: start the backend AND run the tests
run-e2e.cmd
```

Or start the backend yourself and run Playwright directly:

```bat
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

:: in another terminal
cd e2e
npm test                 :: headless
npm run test:headed      :: watch the browser
npm run test:ui          :: interactive mode
npm run report           :: open the last HTML report
```

Override the target with `E2E_BASE_URL` (default `http://localhost:5173`) and `E2E_API_BASE`
(default `$E2E_BASE_URL/api/v1`).

## Layout

```
e2e/
├── playwright.config.js        configuration (1 worker — the suite mutates shared state)
├── run-e2e.cmd                 starts the backend, waits, runs the suite
└── tests/
    ├── support/
    │   ├── api.js              REST helpers + fixture creation + baseline reset
    │   ├── totp.js             RFC 6238 TOTP so the MFA fixture can sign in
    │   └── global-setup.js     fails fast if the backend is down, then resets to the S1 baseline
    ├── 01-auth.ui.spec.js      S1/S2/S3, wrong password, lockout, captcha, TOTP, logout
    ├── 02-security.ui.spec.js  RBAC, 403, alert layer on top, notification lifecycle, Back button
    ├── 03-student.ui.spec.js   submit/update UX, the four submission rules, results
    ├── 04-teacher.ui.spec.js   create class/assignment, grading
    └── 05-admin.ui.spec.js     users, config, audit export, system page, comparison
```

## Notes

- **1 worker, not parallel**: tests switch the system-wide auth mode and the single seeded database,
  so they must run sequentially. The global setup restores the documented S1 baseline before a run.
- **Isolation**: destructive cases create their own throw-away students (“lock…”, “ui…”) and delete
  them afterwards, so the seeded demo accounts are never left in a broken state.
- **Evidence**: `playwright-report/` (HTML, with traces/screenshots on failure) and `test-results/`.
  Both are git-ignored.
- **Fixtures referenced**: `locked01`, `disabled01`, `student_mfa01` (TOTP secret
  `JBSWY3DPEHPK3PXP`), `student03` (not in CS101), `student02` (graded / attempt-limited
  submissions). See [`../docs/test-cases.md`](../docs/test-cases.md) §1.
