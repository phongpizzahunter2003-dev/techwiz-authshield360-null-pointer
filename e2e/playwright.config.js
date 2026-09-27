// Playwright configuration for the AuthShield 360 browser suite.
//
// Prerequisites (both are started automatically for the frontend, the backend must be up):
//   backend  : cd backend  && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
//   frontend : cd frontend && npm run dev          <- started here by webServer
//
// The `dev` profile uses in-memory H2 and re-seeds the lab fixtures on every start, so a run is
// deterministic. `authshield.expose-otp=true` (dev default) makes OTP codes visible in the UI.
const { defineConfig, devices } = require('@playwright/test')

const UI_ORIGIN = process.env.E2E_BASE_URL || 'http://localhost:5173'

module.exports = defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  // The suite mutates shared system state (global auth mode, lockout counters), so it must not
  // run two tests at the same time.
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  forbidOnly: Boolean(process.env.CI),
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],
  globalSetup: require.resolve('./tests/support/global-setup.js'),
  use: {
    baseURL: UI_ORIGIN,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'en-US',
    timezoneId: 'Asia/Ho_Chi_Minh',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'npm run dev',
      cwd: require('node:path').join(__dirname, '..', 'frontend'),
      url: UI_ORIGIN,
      reuseExistingServer: true,
      timeout: 120_000,
      stdout: 'ignore',
    },
  ],
})
