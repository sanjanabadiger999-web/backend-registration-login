@echo off
REM Creates the reglogin database, tables, and dedicated app user (reglogin/frontend).
REM Double-click this file and enter your MySQL ROOT password when prompted.
setlocal
cd /d "%~dp0"

set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist "%MYSQL%" set "MYSQL=mysql"

echo.
echo Creating reglogin database and user 'reglogin' with password 'frontend' ...
echo.
"%MYSQL%" -u root -p < setup.sql
if errorlevel 1 (
  echo.
  echo [ERROR] MySQL setup failed.
  echo   - "Access denied" means the ROOT password was wrong.
  echo   - "'mysql' is not recognized" means MySQL bin is not on PATH.
  echo.
  pause
  exit /b 1
)
echo.
echo [OK] Database ready. Next run:  powershell -ExecutionPolicy Bypass -File "%~dp0..\run-all.ps1"
pause