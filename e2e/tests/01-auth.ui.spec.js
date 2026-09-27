const { test, expect } = require('@playwright/test')
const api = require('./support/api')
const { totp } = require('./support/totp')

/**
 * Browser-level authentication cases (docs/test-cases.md §2 — AUTH-01..AUTH-32, UI side).
 * The `dev` profile exposes the OTP in the UI, so the wizard can be completed end-to-end.
 */

const MFA_SECRET = 'JBSWY3DPEHPK3PXP' // seeded demo secret for student_mfa01

async function gotoLogin(page) {
  await page.goto('/login')
  await expect(page.locator('#btn-login-submit')).toBeVisible()
}

async function submitCredentials(page, username, password) {
  await page.fill('#txt-username', username)
  await page.fill('#txt-password', password)
  await page.click('#btn-login-submit')
}

/** Read the "Test-environment OTP code: 123456" value rendered inside the sign-in card. */
async function shownOtp(page) {
  const code = page.locator('form').locator('p', { hasText: 'Test-environment OTP code' }).locator('strong')
  await expect(code).toBeVisible()
  return (await code.innerText()).trim()
}

test.describe('Authentication UI — S1 / S2 / S3', () => {
  test.beforeAll(async () => {
    // Several tests request S2/S3 OTPs within the 120s captcha window; raise the threshold so only
    // the dedicated captcha case (AUTH-UI-11) trips it.
    await api.resetBaselineConfig()
    await api.updateConfig({ requireCaptchaAfter: 20 })
  })

  test.afterAll(async () => {
    await api.updateConfig({ requireCaptchaAfter: 3 })
    await api.setMode('S1')
  })

  test('AUTH-UI-01 · visiting the app while signed out redirects to /login', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.locator('#txt-username')).toBeVisible()
    await expect(page.locator('#txt-password')).toBeVisible()
  })

  test('AUTH-UI-02 · S1 login with a valid password reaches the student dashboard', async ({ page }) => {
    await gotoLogin(page)
    await submitCredentials(page, 'student01', 'student123')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
    await expect(page.locator('#btn-logout')).toBeVisible()
  })

  test('AUTH-UI-03 · wrong password shows a generic error and leaves the form usable', async ({ page }) => {
    await gotoLogin(page)
    await submitCredentials(page, 'student11', 'not-the-right-password')
    const alert = page.locator('#lbl-login-error')
    await expect(alert).toBeVisible()
    await expect(alert).toContainText(/Incorrect username or password/i)
    await expect(page.locator('#btn-login-submit')).toBeEnabled()

    // A successful sign-in resets the failed-attempt counter so repeated suite runs never drift.
    await page.fill('#txt-password', 'student123')
    await page.click('#btn-login-submit')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
  })

  test('AUTH-UI-04 · the submit button locks while the sign-in request is in flight', async ({ page }) => {
    await gotoLogin(page)
    await page.route('**/api/v1/auth/login', async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 1_200))
      await route.continue()
    })
    await submitCredentials(page, 'student01', 'student123')
    await expect(page.locator('#btn-login-submit')).toBeDisabled()
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
  })

  test('AUTH-UI-05 · five wrong passwords lock the account (threshold = 5)', async ({ page }) => {
    const user = await api.createTempStudent('lock')
    try {
      await gotoLogin(page)
      for (let attempt = 1; attempt <= 5; attempt++) {
        await page.click('#btn-login-submit')
        await expect(page.locator('#lbl-login-error')).toBeVisible()
      }
      await expect(page.locator('#lbl-login-error')).toContainText(/locked/i)
      await expect(page.locator('#btn-login-submit')).toBeDisabled()
      await expect(page.getByText(/Account is locked\. Please try again in/i)).toBeVisible()
    } finally {
      await api.deleteUser(user.id)
    }
  })

  test('AUTH-UI-06 · a locked account is refused even with the correct password', async ({ page }) => {
    await gotoLogin(page)
    await submitCredentials(page, 'locked01', 'student123')
    await expect(page.locator('#lbl-login-error')).toContainText(/locked/i)
    await expect(page.locator('#btn-login-submit')).toBeDisabled()
  })

  test('AUTH-UI-07 · a disabled account is refused', async ({ page }) => {
    await gotoLogin(page)
    await submitCredentials(page, 'disabled01', 'student123')
    await expect(page.locator('#lbl-login-error')).toContainText(/disabled/i)
  })

  test('AUTH-UI-08 · S2 (Mobile OTP) — the correct code completes the sign-in', async ({ page }) => {
    await api.setMode('S2')
    await gotoLogin(page)
    await submitCredentials(page, 'student01', 'student123')
    await expect(page.locator('#txt-mobile-otp')).toBeVisible({ timeout: 15_000 })
    await page.fill('#txt-mobile-otp', await shownOtp(page))
    await page.click('#btn-mobile-otp-verify')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
  })

  test('AUTH-UI-09 · S2 — a wrong OTP code is rejected and the session is not granted', async ({ page }) => {
    await api.setMode('S2')
    await gotoLogin(page)
    await submitCredentials(page, 'student13', 'student123')
    await expect(page.locator('#txt-mobile-otp')).toBeVisible({ timeout: 15_000 })
    await page.fill('#txt-mobile-otp', '000000')
    await page.click('#btn-mobile-otp-verify')
    await expect(page.getByText(/verification code is incorrect/i)).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('AUTH-UI-10 · the resend button is disabled during the cooldown', async ({ page }) => {
    await api.setMode('S2')
    await gotoLogin(page)
    await submitCredentials(page, 'student01', 'student123')
    await expect(page.locator('#txt-mobile-otp')).toBeVisible({ timeout: 15_000 })
    const resend = page.locator('#btn-mobile-otp-resend')
    await expect(resend).toBeDisabled()
    await expect(resend).toContainText(/Resend code \(\d+s\)/)
  })

  test('AUTH-UI-11 · captcha appears once the OTP-request threshold is exceeded', async ({ page }) => {
    await api.updateConfig({ mode: 'S2', requireCaptchaAfter: 1 })
    try {
      await gotoLogin(page)
      await submitCredentials(page, 'student14', 'student123')
      await expect(page.locator('#txt-mobile-otp')).toBeVisible({ timeout: 15_000 })
      // Back to step 1 and request again → the challenge is now required.
      await page.click('#btn-mobile-otp-back')
      await submitCredentials(page, 'student14', 'student123')
      await expect(page.locator('#txt-captcha')).toBeVisible({ timeout: 15_000 })
      await expect(page.getByText(/Bot verification/i)).toBeVisible()
      await expect(page.getByText(/complete the challenge/i)).toBeVisible()
    } finally {
      await api.updateConfig({ requireCaptchaAfter: 20 })
    }
  })

  test('AUTH-UI-12 · S3 (Mobile + Email OTP) completes both steps', async ({ page }) => {
    await api.setMode('S3')
    await gotoLogin(page)
    await submitCredentials(page, 'student01', 'student123')
    await expect(page.locator('#txt-mobile-otp')).toBeVisible({ timeout: 15_000 })
    await page.fill('#txt-mobile-otp', await shownOtp(page))
    await page.click('#btn-mobile-otp-verify')
    await expect(page.locator('#txt-email-otp')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/Mobile OTP verification succeeded/i)).toBeVisible()
    await page.fill('#txt-email-otp', await shownOtp(page))
    await page.click('#btn-email-otp-verify')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
  })

  test('AUTH-UI-13 · a real TOTP authenticator code signs the MFA fixture in', async ({ page }) => {
    await api.setMode('S2')
    await gotoLogin(page)
    await submitCredentials(page, 'student_mfa01', 'student123')
    await expect(page.locator('#txt-mobile-otp')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/authenticator app/i)).toBeVisible()
    await expect(page.locator('#btn-mobile-otp-resend')).toBeDisabled()
    await page.fill('#txt-mobile-otp', totp(MFA_SECRET))
    await page.click('#btn-mobile-otp-verify')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
  })

  test('AUTH-UI-14 · logout clears the session and browser Back cannot restore the dashboard', async ({ page }) => {
    await gotoLogin(page)
    await submitCredentials(page, 'student01', 'student123')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
    await page.click('#btn-logout')
    await expect(page).toHaveURL(/\/login$/, { timeout: 15_000 })
    await page.goBack()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.locator('#btn-login-submit')).toBeVisible()
  })
})
