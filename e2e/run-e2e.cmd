@echo off
rem ============================================================================
rem  AuthShield 360 - one-shot E2E runner (backend + frontend + Playwright)
rem
rem  Starts the backend with the `dev` profile (in-memory H2, re-seeded on every
rem  start so the run is deterministic), waits for port 8080, installs the
rem  Playwright browser if needed and runs the suite.
rem ============================================================================
setlocal
cd /d "%~dp0"

set "BACKEND=%~dp0..\backend"
set "FRONTEND=%~dp0..\frontend"

echo [e2e] Starting the backend (dev profile) in a new window...
start "AuthShield360 backend (dev)" cmd /k "cd /d "%BACKEND%" && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev"

echo [e2e] Waiting for http://localhost:8080 ...
powershell -NoProfile -Command "for ($i=0; $i -lt 120; $i++) { if (Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 }; Start-Sleep -Seconds 2 }; exit 1"
if errorlevel 1 (
  echo [e2e] The backend did not start in time. Check the backend window for errors.
  exit /b 1
)
echo [e2e] Backend is up.

if not exist node_modules (
  echo [e2e] Installing Playwright dependencies...
  call npm install
)
call npx playwright install chromium

echo [e2e] Running the Playwright suite...
call npx playwright test
set "RESULT=%ERRORLEVEL%"

echo [e2e] Done. HTML report: %~dp0playwright-report\index.html
endlocal & exit /b %RESULT%
