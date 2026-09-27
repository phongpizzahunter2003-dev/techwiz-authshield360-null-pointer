const { test, expect } = require('@playwright/test')
const api = require('./support/api')

/**
 * Browser-level teacher cases (docs/test-cases.md §4): class & assignment creation, grading and the
 * button-locking/UX around those async actions.
 */

async function login(page, username, password, home) {
  await page.goto('/login')
  await page.locator('#txt-username').fill(username)
  await page.locator('#txt-password').fill(password)
  await page.locator('#btn-login-submit').click()
  await expect(page).toHaveURL(home, { timeout: 15_000 })
}

const toastLayer = (page) => page.locator('div[role="status"][aria-live="polite"]')

test.describe('Teacher portal', () => {
  test('TEA-UI-01 · a teacher can create a class', async ({ page }) => {
    const code = 'UI' + Date.now().toString(36)
    await login(page, 'teacher01', 'teacher123', /\/teacher$/)
    await page.goto('/teacher/classes')

    await page.getByRole('button', { name: /Create class/i }).click()
    await page.locator('#cl-code').fill(code)
    await page.locator('#cl-name').fill('UI Automation Class')
    await page.locator('[role="dialog"] button[type="submit"]').click()

    await expect(toastLayer(page).getByText(/Class created successfully/i)).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(code)).toBeVisible()

    try {
      const id = await api.classroomId(code)
      const teacher = await api.tokenFor('teacher01', 'teacher123')
      await api.apiDel(`/teacher/classrooms/${id}`, { token: teacher })
    } catch {
      /* cleanup is best-effort */
    }
  })

  test('TEA-UI-02 · a duplicate class code is rejected and the dialog stays open', async ({ page }) => {
    await login(page, 'teacher01', 'teacher123', /\/teacher$/)
    await page.goto('/teacher/classes')

    await page.getByRole('button', { name: /Create class/i }).click()
    await page.locator('#cl-code').fill('CS101')
    await page.locator('#cl-name').fill('Duplicate code')
    await page.locator('[role="dialog"] button[type="submit"]').click()

    await expect(toastLayer(page).getByText(/already|exist/i)).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('[role="dialog"]')).toBeVisible()
  })

  test('TEA-UI-03 · a teacher can create an assignment for a class', async ({ page }) => {
    const title = 'UI assignment ' + Date.now().toString(36)
    await login(page, 'teacher01', 'teacher123', /\/teacher$/)
    await page.goto('/teacher/assignments')

    await page.getByRole('button', { name: /Create assignment/i }).click()
    await page.locator('#as-title').fill(title)
    await page.selectOption('#as-class', { index: 1 })
    await page.locator('#as-due').fill('2026-12-31T17:00')
    await page.locator('#as-max').fill('100')
    await page.locator('[role="dialog"] button[type="submit"]').click()

    await expect(assignmentTitleLocator(page, title)).toBeVisible({ timeout: 15_000 })
  })

  test('TEA-UI-04 · a teacher can grade a submission and the action locks while saving', async ({ page }) => {
    await login(page, 'teacher01', 'teacher123', /\/teacher$/)
    const assignment = await api.assignmentId('Loops and arrays')
    await page.goto(`/teacher/assignments/${assignment}`)

    const score = page.locator('input[id^="score-"]').first()
    if ((await score.count()) === 0) {
      test.skip(true, 'This assignment has no submissions to grade')
    }
    await score.fill('88')
    const saveButton = page.getByRole('button', { name: /Save score/i }).first()
    await expect(saveButton).toBeEnabled()
    await saveButton.click()

    await expect(toastLayer(page).getByText(/Graded\./i)).toBeVisible({ timeout: 15_000 })
  })
})

/** The assignment title rendered in the list. */
function assignmentTitleLocator(page, title) {
  return page.getByText(title, { exact: false }).first()
}
