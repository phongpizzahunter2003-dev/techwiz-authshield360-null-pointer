const fs = require('node:fs')
const { test, expect } = require('@playwright/test')
const api = require('./support/api')

/**
 * Browser-level administrator cases (docs/test-cases.md §5): user management, configuration,
 * audit log export and the system page.
 */

async function login(page, username, password, home) {
  await page.goto('/login')
  await page.locator('#txt-username').fill(username)
  await page.locator('#txt-password').fill(password)
  await page.locator('#btn-login-submit').click()
  await expect(page).toHaveURL(home, { timeout: 15_000 })
}

const toastLayer = (page) => page.locator('div[role="status"][aria-live="polite"]')

test.describe('Administrator portal', () => {
  test.afterAll(async () => {
    await api.setMode('S1')
  })

  test('ADM-UI-01 · the user list paginates and can be searched', async ({ page }) => {
    await login(page, 'admin01', 'admin123', /\/admin$/)
    await page.goto('/admin/users')
    await expect(page.getByRole('heading', { name: /Users/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('table tbody tr').first()).toBeVisible()

    await page.locator('#user-search').fill('student01')
    await page.locator('#user-search').press('Enter')
    await expect(page.locator('table tbody tr').first()).toContainText('student01', { timeout: 15_000 })
  })

  test('ADM-UI-02 · an administrator can create a user through the dialog', async ({ page }) => {
    const username = 'uiadm' + Date.now().toString(36)
    await login(page, 'admin01', 'admin123', /\/admin$/)
    await page.goto('/admin/users')

    await page.getByRole('button', { name: /Add user/i }).click()
    await page.locator('#u-username').fill(username)
    await page.locator('#u-email').fill(`${username}@mailtrap.io`)
    await page.locator('#u-fullname').fill('UI Created User')
    await page.locator('#u-password').fill('student123')
    await page.locator('#u-role').selectOption('STUDENT')
    await page.locator('[role="dialog"] button[type="submit"]').click()

    await expect(toastLayer(page).getByText(/Account created successfully/i)).toBeVisible({ timeout: 15_000 })

    try {
      const id = await api.findUserId(username)
      if (id) await api.deleteUser(id)
    } catch {
      /* cleanup is best-effort */
    }
  })

  test('ADM-UI-03 · the global authentication mode can be switched and persists', async ({ page }) => {
    await login(page, 'admin01', 'admin123', /\/admin$/)
    await page.goto('/admin/config')
    try {
      await page.getByRole('button', { name: /S2 — Password \+ OTP/i }).click()
      await page.getByRole('button', { name: /Save configuration/i }).click()
      await expect(toastLayer(page).getByText(/Configuration saved/i)).toBeVisible({ timeout: 15_000 })

      await page.reload()
      await expect(page.getByText(/Selected:/)).toContainText('S2', { timeout: 15_000 })
      const saved = await api.getConfig()
      expect(saved.mode).toBe('S2')
    } finally {
      await api.setMode('S1')
    }
  })

  test('ADM-UI-04 · the audit CSV export downloads with masked personal data', async ({ page }) => {
    await login(page, 'admin01', 'admin123', /\/admin$/)
    await page.goto('/admin/audit-logs')
    await expect(page.getByRole('heading', { name: /Audit/i })).toBeVisible({ timeout: 15_000 })

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: /Export CSV/i }).click(),
    ])
    expect(download.suggestedFilename()).toMatch(/^auth_logs_.*\.csv$/)

    const file = await download.path()
    const csv = fs.readFileSync(file, 'utf8')
    expect(csv).not.toContain('student01@mailtrap.io')
    expect(csv).not.toContain('0912002001')
  })

  test('ADM-UI-05 · the system page reports the database and per-table row counts', async ({ page }) => {
    await login(page, 'admin01', 'admin123', /\/admin$/)
    await page.goto('/admin/system')
    await expect(page.getByText('Database connection')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Rows per table')).toBeVisible()
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('ADM-UI-06 · the S1/S2/S3 comparison page renders all three modes', async ({ page }) => {
    await login(page, 'admin01', 'admin123', /\/admin$/)
    await page.goto('/admin/comparison')
    await expect(page.getByText(/\bS1\b/).first()).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/\bS2\b/).first()).toBeVisible()
    await expect(page.getByText(/\bS3\b/).first()).toBeVisible()
  })
})
