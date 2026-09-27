/**
 * Thin REST helpers for the AuthShield 360 E2E suite.
 *
 * The SPA proxies `/api` through Vite, so the API base defaults to the UI origin — no CORS and
 * only one host/port to remember. Override with E2E_BASE_URL / E2E_API_BASE.
 */
const { request } = require('@playwright/test')

const ORIGIN = process.env.E2E_BASE_URL || 'http://localhost:5173'
const API = process.env.E2E_API_BASE || `${ORIGIN}/api/v1`

async function context(token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  return request.newContext({ baseURL: API, extraHTTPHeaders: headers })
}

async function readBody(res) {
  const text = await res.text()
  try {
    return JSON.parse(text)
  } catch {
    return { raw: text }
  }
}

async function call(method, path, { data, token, params } = {}) {
  const ctx = await context(token)
  try {
    const res = await ctx[method](path, { data, params })
    return { status: res.status(), ok: res.ok(), body: await readBody(res), headers: res.headers() }
  } finally {
    await ctx.dispose()
  }
}

const apiGet = (path, options) => call('get', path, options)
const apiPost = (path, options) => call('post', path, options)
const apiPut = (path, options) => call('put', path, options)
const apiDel = (path, options) => call('delete', path, options)

/** Login and return the raw HTTP result (never throws) so tests can assert error codes. */
const loginRaw = (username, password, extra = {}) =>
  apiPost('/auth/login', { data: { username, password, ...extra } })

/** Login and return the payload `data`; throws a readable error otherwise. */
async function login(username, password, extra = {}) {
  const r = await loginRaw(username, password, extra)
  if (!r.ok || r.body?.data === undefined) {
    const err = new Error(r.body?.message || `login failed with HTTP ${r.status}`)
    err.code = r.body?.code
    err.fields = r.body?.fields
    err.status = r.status
    throw err
  }
  return r.body.data
}

async function tokenFor(username, password) {
  const data = await login(username, password)
  if (!data.token) throw new Error(`No token returned for ${username} (status=${data.status})`)
  return data.token
}

const adminToken = () => tokenFor('admin01', 'admin123')

async function getConfig(token = null) {
  const t = token || (await adminToken())
  const r = await apiGet('/admin/config', { token: t })
  if (!r.ok) throw new Error(`GET /admin/config failed (${r.status}): ${JSON.stringify(r.body)}`)
  return r.body.data
}

/** Merge a patch into the current configuration (the PUT endpoint expects the full object). */
async function updateConfig(patch, token = null) {
  const t = token || (await adminToken())
  const current = await getConfig(t)
  const payload = {
    mode: current.mode,
    otpType: current.otpType,
    otpLength: Number(current.otpLength),
    otpValiditySeconds: Number(current.otpValiditySeconds),
    resendCooldownSeconds: Number(current.resendCooldownSeconds),
    maxResend: Number(current.maxResend),
    maxFailedAttempts: Number(current.maxFailedAttempts),
    lockoutDurationsSeconds: current.lockoutDurationsSeconds,
    requireCaptchaAfter: Number(current.requireCaptchaAfter),
    smtpHost: current.smtpHost,
    smtpPort: current.smtpPort ? Number(current.smtpPort) : null,
    smtpUsername: current.smtpUsername,
    smtpFrom: current.smtpFrom,
    emailOtpEnabled: current.emailOtpEnabled,
    ...patch,
  }
  const r = await apiPut('/admin/config', { data: payload, token: t })
  if (!r.ok) throw new Error(`PUT /admin/config failed (${r.status}): ${JSON.stringify(r.body)}`)
  return r.body.data
}

const setMode = (mode) => updateConfig({ mode })

/** The configuration the fixtures were designed against (see docs/test-cases.md §1.4). */
const BASELINE = {
  mode: 'S1',
  otpType: 'TOTP',
  otpLength: 6,
  otpValiditySeconds: 90,
  resendCooldownSeconds: 60,
  maxResend: 3,
  maxFailedAttempts: 5,
  lockoutDurationsSeconds: '60,300,900',
  requireCaptchaAfter: 3,
  emailOtpEnabled: true,
}

const resetBaselineConfig = (token = null) => updateConfig(BASELINE, token)

let seq = 0

/** Create a throwaway student so destructive tests never disturb the seeded fixtures. */
async function createTempStudent(prefix = 'e2e') {
  const t = await adminToken()
  const username = `${prefix}${Date.now().toString(36)}${(seq++).toString(36)}`
  const r = await apiPost('/admin/users', {
    token: t,
    data: {
      username,
      email: `${username}@mailtrap.io`,
      phone: `0909${String(Date.now()).slice(-6)}`,
      fullName: `E2E Temp ${username}`,
      password: 'student123',
      role: 'STUDENT',
      authMode: 'INHERIT',
    },
  })
  if (!r.ok) throw new Error(`createTempStudent failed (${r.status}): ${JSON.stringify(r.body)}`)
  return { username, password: 'student123', id: r.body.data?.id }
}

async function deleteUser(id) {
  const t = await adminToken()
  return apiDel(`/admin/users/${id}`, { token: t })
}

/** Look up the numeric id of a seeded classroom by its code (e.g. "CS101"). */
async function classroomId(code) {
  const t = await adminToken()
  const r = await apiGet('/classrooms', { token: t })
  if (!r.ok) throw new Error(`GET /classrooms failed (${r.status})`)
  const found = (r.body.data || []).find((c) => c.code === code)
  if (!found) throw new Error(`Classroom ${code} not found`)
  return found.id
}

/** Look up the numeric id of a seeded assignment by a fragment of its title. */
async function assignmentId(titleFragment) {
  const t = await adminToken()
  const r = await apiGet('/assignments', { token: t })
  if (!r.ok) throw new Error(`GET /assignments failed (${r.status})`)
  const found = (r.body.data || []).find((a) => String(a.title).includes(titleFragment))
  if (!found) throw new Error(`Assignment matching "${titleFragment}" not found`)
  return found.id
}

/** Find a user id by exact username (for cleanup after UI creation). */
async function findUserId(username) {
  const t = await adminToken()
  const r = await apiGet('/admin/users', { token: t, params: { q: username, size: 50 } })
  if (!r.ok) return null
  const found = (r.body.data?.items || []).find((u) => u.username === username)
  return found ? found.id : null
}

/** Fail fast, with a helpful message, when the backend is down or not seeded. */
async function assertBackendReady() {
  try {
    await adminToken()
  } catch (e) {
    throw new Error(
      `Backend not reachable or not seeded at ${API}.\n` +
        'Start it with:  cd backend && .\\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"\n' +
        `Underlying error: ${e.message}`,
    )
  }
}

module.exports = {
  ORIGIN,
  API,
  apiGet,
  apiPost,
  apiPut,
  apiDel,
  loginRaw,
  login,
  tokenFor,
  adminToken,
  getConfig,
  updateConfig,
  setMode,
  resetBaselineConfig,
  createTempStudent,
  deleteUser,
  classroomId,
  assignmentId,
  findUserId,
  assertBackendReady,
}
