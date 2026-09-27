const { test, expect } = require('@playwright/test')
const api = require('./support/api')

/**
 * Browser-level security / cross-cutting cases (docs/test-cases.md §2, §6 and the X-12 requirement
 * that alerts show on top of the whole system). RBAC, notification lifecycle, alert layer, Back.
 */

async function login(page, username, password) {
  await page.goto('/login')
  await page.locator('#txt-username').fill(username)
  await page.locator('#txt-password').fill(password)
  await page.locator('#btn-login-submit').click()
}

test.describe('Security, RBAC, notifications and the alert layer', () => {
  test.afterAll(async () => {
    await api.setMode('S1')
  })

  test('SECU-UI-01 · a student who opens /admin is redirected to the 403 page', async ({ page }) => {
    await login(page, 'student01', 'student123')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/403$/, { timeout: 15_000 })
    await expect(page.getByText(/You do not have permission to access this resource/i)).toBeVisible()
  })

  test('SECU-UI-02 · a teacher only sees their own classes\' assignments', async ({ page }) => {
    await login(page, 'teacher02', 'teacher123')
    await page.goto('/teacher/assignments')
    await expect(page.getByRole('heading', { name: /Assignments/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Assignment 1 - Loops and arrays')).toHaveCount(0)
  })

  test('SECU-UI-03 · the alert layer is pinned to the very top, above the app chrome', async ({ page }) => {
    await login(page, 'student01', 'student123')
    await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })

    const layer = page.locator('div[role="status"][aria-live="polite"]')
    await expect(layer).toHaveCount(1)
    const style = await layer.evaluate((el) => {
      const s = getComputedStyle(el)
      return { position: s.position, top: s.top, zIndex: Number(s.zIndex), pointerEvents: s.pointerEvents }
    })
    expect(style.position).toBe('fixed')
    expect(style.top).toBe('0px')
    expect(style.zIndex).toBeGreaterThanOrEqual(9999)
    expect(style.pointerEvents).toBe('none') // the header underneath stays clickable
  })

  test('SECU-UI-04 · new notifications raise an alert inside that top layer', async ({ page }) => {
    const temp = await api.createTempStudent('notif')
    try {
      const teacher = await api.tokenFor('teacher01', 'teacher123')
      const classroom = await api.classroomId('CS101')
      const enrolled = await api.apiPost(`/teacher/classrooms/${classroom}/enroll`, {
        token: teacher,
        data: { studentId: temp.id },
      })
      expect(enrolled.ok).toBeTruthy()

      await login(page, temp.username, temp.password)
      await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
      await expect(page.locator('#btn-notifications')).toHaveAttribute('aria-label', /Notifications \(\d+ unread\)/)

      await page.goto('/notifications')
      await page.getByRole('button', { name: /Mark all as read/i }).click()
      const layer = page.locator('div[role="status"][aria-live="polite"]')
      await expect(layer.getByText(/All notifications marked as read/i)).toBeVisible()
    } finally {
      await api.deleteUser(temp.id)
    }
  })

  test('SECU-UI-05 · the notification bell badge disappears once everything is read', async ({ page }) => {
    const temp = await api.createTempStudent('bell')
    try {
      const teacher = await api.tokenFor('teacher01', 'teacher123')
      const classroom = await api.classroomId('CS101')
      await api.apiPost(`/teacher/classrooms/${classroom}/enroll`, { token: teacher, data: { studentId: temp.id } })

      await login(page, temp.username, temp.password)
      const bell = page.locator('#btn-notifications')
      await expect(bell).toHaveAttribute('aria-label', /Notifications \(\d+ unread\)/, { timeout: 15_000 })

      await page.goto('/notifications')
      await page.getByRole('button', { name: /Mark all as read/i }).click()
      await expect(page.getByText(/All notifications marked as read/i)).toBeVisible()
      await expect(page.locator('#btn-notifications')).toHaveAttribute('aria-label', 'Notifications')
    } finally {
      await api.deleteUser(temp.id)
    }
  })

  test('SECU-UI-06 · every list item opens a detail page that has a working Back button', async ({ page }) => {
    await login(page, 'student01', 'student123')
    await page.goto('/student/assignments')
    const firstLink = page.locator('a[href^="/student/assignments/"]').first()
    await expect(firstLink).toBeVisible({ timeout: 15_000 })
    await firstLink.click()
    await expect(page).toHaveURL(/\/student\/assignments\/\d+/)
    await page.getByRole('button', { name: /Back/i }).first().click()
    await expect(page).toHaveURL(/\/student\/assignments$/)
  })
})
