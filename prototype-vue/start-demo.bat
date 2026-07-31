@echo off
setlocal

cd /d "%~dp0"

echo ==========================================
echo VAD Demo Launcher
echo ==========================================
echo Working directory: %cd%
echo.

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm was not found. Please install Node.js first.
  echo.
  pause
  exit /b 1
)

if not exist "node_modules" (
  echo [1/3] node_modules not found. Installing dependencies...
  call npm install
  if errorlevel 1 (
    echo.
    echo [ERROR] npm install failed.
    pause
    exit /b 1
  )
) else (
  echo [1/3] Dependencies already installed.
)

echo [2/3] Starting local demo server in a new window...
start "VAD Demo Server" cmd /k "cd /d ""%~dp0"" && npm run dev -- --host 0.0.0.0 --port 5173"
if errorlevel 1 (
  echo.
  echo [ERROR] Failed to start the demo server window.
  pause
  exit /b 1
)

echo [3/3] Waiting for the dev server...
timeout /t 5 >nul

echo Opening demo entrance: http://localhost:5173/demo
start "" "http://localhost:5173/demo"

echo.
echo Demo server should now be running.
echo If the browser did not open, visit:
echo http://localhost:5173/demo
echo.
pause
