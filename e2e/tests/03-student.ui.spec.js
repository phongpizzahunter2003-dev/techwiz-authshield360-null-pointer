const { test, expect } = require('@playwright/test')
const api = require('./support/api')

/**
 * Browser-level student cases (docs/test-cases.md §3): the submit / update UX, the four submission
 * rules, data scoping and exam results.
 */

async function login(page, username, password) {
  await page.goto('/login')
  await page.locator('#txt-username').fill(username)
  await page.locator('#txt-password').fill(password)
  await page.locator('#btn-login-submit').click()
  await expect(page).toHaveURL(/\/student$/, { timeout: 15_000 })
}

async function enrolledTempStudent() {
  const temp = await api.createTempStudent('ui')
  const teacher = await api.tokenFor('teacher01', 'teacher123')
  const classroom = await api.classroomId('CS101')
  await api.apiPost(`/teacher/classrooms/${classroom}/enroll`, { token: teacher, data: { studentId: temp.id } })
  return temp
}

test.describe('Student portal', () => {
  test('STU-UI-01 · the submit button stays locked until a file is chosen, then submits', async ({ page }) => {
    const temp = await enrolledTempStudent()
    try {
      const assignment = await api.assignmentId('Loops and arrays')
      await login(page, temp.username, temp.password)
      await page.goto(`/student/assignments/${assignment}`)

      const submit = page.getByRole('button', { name: /^(Submit|Resubmit)/ })
      await expect(submit).toBeVisible({ timeout: 15_000 })
      await expect(submit).toBeDisabled() // no file yet

      await page.setInputFiles('input[type="file"]', {
        name: 'answer.txt',
        mimeType: 'text/plain',
        buffer: Buffer.from('playwright submission'),
      })
      await expect(submit).toBeEnabled()
      await submit.click()

      await expect(page.getByText(/Submitted successfully/i)).toBeVisible({ timeout: 15_000 })
      await expect(page.getByRole('cell', { name: 'answer.txt' })).toBeVisible()
    } finally {
      await api.deleteUser(temp.id)
    }
  })

  test('STU-UI-02 · a closed assignment shows the UC-A3 "cannot submit" panel', async ({ page }) => {
    await login(page, 'student01', 'student123')
    const assignment = await api.assignmentId('Midterm exam')
    await page.goto(`/student/assignments/${assignment}`)
    await expect(page.getByRole('heading', { name: /Cannot submit/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/This assignment is closed/i)).toBeVisible()
  })

  test('STU-UI-03 · an already-graded submission cannot be updated (student02 / A4)', async ({ page }) => {
    await login(page, 'student02', 'student123')
    const assignment = await api.assignmentId('Resubmission allowed')
    await page.goto(`/student/assignments/${assignment}`)
    await expect(page.getByRole('heading', { name: /Cannot submit/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/Resubmission is not allowed/i)).toBeVisible()
  })

  test('STU-UI-04 · the attempt-limit message is shown (student02 / single attempt)', async ({ page }) => {
    await login(page, 'student02', 'student123')
    const assignment = await api.assignmentId('Single attempt only')
    await page.goto(`/student/assignments/${assignment}`)
    await expect(page.getByRole('heading', { name: /Cannot submit/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/maximum number of submissions/i)).toBeVisible()
  })

  test('STU-UI-05 · a student outside CS101 cannot see its assignments (data scoping)', async ({ page }) => {
    await login(page, 'student03', 'student123')
    await page.goto('/student/assignments')
    await expect(page.getByRole('heading', { name: /Assignments/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Assignment 1 - Loops and arrays')).toHaveCount(0)
  })

  test('STU-UI-06 · the student sees their own exam results', async ({ page }) => {
    await login(page, 'student01', 'student123')
    await page.goto('/student/results')
    await expect(page.getByRole('heading', { name: /Exam results/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('STU-UI-07 · the assignment list links into a detail page with a Back button', async ({ page }) => {
    await login(page, 'student01', 'student123')
    await page.goto('/student/assignments')
    const link = page.locator('a[href^="/student/assignments/"]').first()
    await expect(link).toBeVisible({ timeout: 15_000 })
    await link.click()
    await expect(page).toHaveURL(/\/student\/assignments\/\d+/)
    await expect(page.getByText('Assignment information')).toBeVisible()
    await page.getByRole('button', { name: /Back/i }).first().click()
    await expect(page).toHaveURL(/\/student\/assignments$/)
  })
})
