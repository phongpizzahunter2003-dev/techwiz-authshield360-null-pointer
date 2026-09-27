# fe-rules.md — Frontend Rules (React)

> Owner: Senior Frontend / UX · Stack: **React 18 + Vite + React Router 6 + Tailwind CSS + Axios**
> Status: **Baseline v1.0** · Language: JavaScript (jsx), not TypeScript.

## 1. Design language — "young, bright, friendly"

The SPA must look modern, energetic and approachable while staying clean and accessible.

### 1.1 Colour tokens (Tailwind theme)
| Token | Hex | Use |
|---|---|---|
| `brand.500` | `#6C5CE7` | primary actions, active nav |
| `brand.600` | `#5B4BD5` | hover |
| `accent.400` | `#00D2A8` | success/energy highlights |
| `sun.400` | `#FFC93C` | warnings, badges |
| `coral.400` | `#FF6B6B` | errors, destructive |
| `sky.400` | `#4FC3F7` | info, charts |
| `surface` | `#FFFFFF` | cards, page base |
| `surface.soft` | `#F7F8FF` | app background |
| `ink.900` | `#1B1B2F` | headings |
| `ink.600` | `#4A4A68` | body |
| `ink.400` | `#9A9AB0` | muted |

Rules: **white / very light backgrounds**, vivid accents, never muddy. Gradients allowed only for hero/login.
Contrast must meet **WCAG AA (4.5:1)** for text.

### 1.2 Shape, depth, motion
- Radii: cards `rounded-2xl`, buttons `rounded-xl`, pills `rounded-full`.
- Shadows: soft (`shadow-soft`), never harsh.
- Spacing scale: 4/8/12/16/24/32.
- Motion: 150–250 ms ease-out; respect `prefers-reduced-motion`.
- Illustrative emoji/icon accents are encouraged (youthful) but must not replace labels.

### 1.3 Typography
Inter (or system stack) · h1 `text-2xl/3xl font-bold`, body `text-sm/base`, min 12px.

## 2. Responsive rules

- **Mobile-first.** Breakpoints: `sm 640 · md 768 · lg 1024 · xl 1280`.
- Layout: 1 column < 768, 2 at md, 3–4 at lg for card grids.
- Sidebar collapses to a bottom/drawer nav on mobile; tables become stacked cards.
- All interactive targets ≥ 44×44 px.
- No horizontal scroll at 320 px width.

## 3. Project structure

```
frontend/src
├── api/           axios instance + endpoint modules (auth, users, assignments, audit, admin)
├── app/           App.jsx, router, providers
├── components/     ui/ (Button, Card, Input, Modal, Table, Badge, Toast, Spinner, Countdown)
│                   layout/ (AppShell, Sidebar, Topbar, PageHeader)
├── context/       AuthContext, ThemeContext
├── features/      auth/ · student/ · teacher/ · admin/ (feature-first)
├── hooks/         useAuth, useCountdown, useDebounce, useFetch
├── i18n/          messages.js (canonical VI strings)
└── styles/        index.css (Tailwind layers + tokens)
```

## 4. Component rules

1. Function components + hooks only.
2. One component per file; file name = component name (`PascalCase.jsx`).
3. Props are explicit; no prop drilling beyond 2 levels — use context.
4. Controlled inputs; forms validated client-side **and** server-side.
5. Every list has empty / loading / error states.
6. No business rule enforced *only* on the client (BR-05).

## 5. Mandatory UI element IDs (from the functional spec)

These exact IDs **must** exist so automated/BA test evidence matches the spec:

**Login (UC-01/02/03):** `txt-username`, `txt-password`, `btn-login-submit`, `lbl-login-error`,
`txt-mobile-otp`, `btn-mobile-otp-verify`, `btn-mobile-otp-resend`, `lbl-mobile-otp-countdown`,
`btn-mobile-otp-back`, `txt-email-otp`, `btn-email-otp-verify`, `btn-email-otp-resend`,
`lbl-email-otp-countdown`, `btn-email-otp-restart`, `btn-logout`, `btn-confirm-export-limit`.

## 6. Auth & session UX

- Multi-step wizard driven by the login state machine; each step has its own route guard.
- Countdown timers are real (`useCountdown`), format `mm:ss`.
- Resend button: immediately `disabled` + label "Đang gửi..."; re-enabled only at 0 s; server 429 shown as toast.
- Lockout shows live mm:ss countdown and disables submit.
- Logout: call API → clear `sessionStorage`, `localStorage.auth_token`, cookies → `window.location.replace('/login')` (history invalidation, UC-06).
- On `401`: force logout + redirect with `SESSION_EXPIRED` message.
- On `403`: show `FORBIDDEN` message; do not leak resource details.

## 7. Routing & guards

Routes are grouped by role. `<ProtectedRoute roles={['ADMIN']}>` hides UI, but the server is the
real gate. Unauthorized direct URL access shows a friendly 403 page, never the protected view.

```
/login                     public (S1/S2/S3 wizard)
/student/*                 STUDENT
/teacher/*                 TEACHER
/admin/*                   ADMIN (users, config, audit, comparison)
```

## 8. Charts & drill-down navigation (added after review)

Every dashboard shows **role-specific, clickable charts**; every displayed item is navigable.

### 8.1 Rules

1. Each role has its **own chart logic** — the charts must not be a generic copy:
   | Role | Charts | Drill-down target |
   |---|---|---|
   | Student | PIE assignment status (on-time / late / pending / locked); BAR attempts per assignment; BAR score per exam | `/student/assignments?bucket=…`, `/student/assignments/:id`, `/student/results` |
   | Teacher | BAR submissions per assignment; PIE grading progress (graded vs awaiting); BAR students per class | `/teacher/assignments/:id`, `/teacher/classes/:id` |
   | Admin | BAR successful logins by mode S1/S2/S3; PIE users by role; BAR security events (login failures, lockouts, OTP failures, privilege violations, session replay); LINE events over the last 7 days | `/admin/audit-logs?mode=…&action=…&from=…&to=…`, `/admin/users?role=…` |
2. A chart element (bar / pie slice / line point) **and** its legend chip must be clickable and navigate to the matching detail view. Legend chips are the keyboard-accessible equivalent (colour is never the only signal).
3. Charts are **not animated on mount** (`isAnimationActive={false}`) so they render deterministically and respect `prefers-reduced-motion`.
4. Charts must render loading / empty / error states; an all-zero series shows an explicit "Chưa có dữ liệu" message.
5. Chart data comes from the API (`/api/v1/analytics/{student|teacher|admin}`) so drill links stay consistent with server-side authorisation.

### 8.2 Detail page & back button contract

- Every detail view is wrapped in `DetailShell`, which always renders a **Back button**.
- `BackButton` uses `navigate(-1)` when real history exists, otherwise a deterministic fallback route (`fallback` prop) so a directly-opened URL is never a dead end.
- Detail routes: `/student/assignments/:id`, `/teacher/assignments/:id`, `/teacher/classes/:id`, `/admin/users/:id`, `/admin/audit-logs/:id`, `/admin/system`.
- List pages accept drill-down **query params** (`?bucket=`, `?role=`, `?action=`, `?mode=`, `?from=`, `?to=`) so a chart click lands on a pre-filtered view.
- Never rely on the client filter for authorisation — the detail endpoints re-check the role server-side (BR-05).

## 9. Data & state

- Server state via small fetch hooks + Axios; no global cache library required.
- Auth state in `AuthContext` (user, role, token) hydrated from `sessionStorage`.
- Errors normalised from the `ApiResponse` envelope into `{ code, message, fields }`.
- Tables: pagination (≤ 50 rows/page per UC-11), sortable headers, filter chips.

## 9. Accessibility & quality

- Semantic landmarks, labels tied to inputs, `aria-live` for errors/countdowns.
- Keyboard operable modals (focus trap, Esc), visible focus rings.
- Colour never the sole signal (icon + text).
- Lint clean (`eslint`), no console noise in production build.

## 10. Definition of Done (frontend)

Responsive at 320/768/1280; all mandatory IDs present; loading/empty/error states; AA contrast;
server-side errors surfaced; no secret in the bundle; feature matches the matching UC + FE test.
