# ============================================================================
# run-all.ps1 - Builds (if needed) and starts the three reglogin applications.
#   FrontendRegLogin       -> http://localhost:8080  (Signup / Login / Home)
#   UserService            -> http://localhost:8081  (POST /api/reg)
#   AuthenticationService  -> http://localhost:8082  (login/me/logout + JWT cookie)
#
# Run:  powershell -ExecutionPolicy Bypass -File .\run-all.ps1
# Logs are written to the `logs/` folder.
# ============================================================================

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Get-JarPath([string]$proj, [string]$artifact) {
    $suffix = if ((Test-Path (Join-Path $root "$proj\target\$artifact.jar")) -eq $false) { $true } else { $false }
    return Join-Path $root "$proj\target\$artifact.jar"
}

$apps = @(
    @{ Name = 'FrontendRegLogin';         Port = 8080; Artifact = 'frontend-reg-login-1.0.0'          },
    @{ Name = 'UserService';              Port = 8081; Artifact = 'user-service-1.0.0'                 },
    @{ Name = 'AuthenticationService';    Port = 8082; Artifact = 'authentication-service-1.0.0'       }
)

$started = @()

foreach ($app in $apps) {
    $jar = Get-JarPath $app.Name $app.Artifact

    if (-not (Test-Path $jar)) {
        Write-Host "Building $($app.Name) ..." -ForegroundColor Yellow
        Push-Location (Join-Path $root $app.Name)
        mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Build failed for $($app.Name). Aborting." -ForegroundColor Red
            Pop-Location
            exit 1
        }
        Pop-Location
    }

    $inUse = Get-NetTCPConnection -LocalPort $app.Port -State Listen -ErrorAction SilentlyContinue
    if ($inUse) {
        Write-Host "[SKIP] $($app.Name) - port $($app.Port) is already in use." -ForegroundColor Magenta
        continue
    }

    $stdOut = Join-Path $logDir "$($app.Name).log"
    $proc = Start-Process -FilePath 'java' -ArgumentList @('-jar', "`"$jar`"") -WindowStyle Hidden `
                -RedirectStandardOutput $stdOut -RedirectStandardError (Join-Path $logDir "$($app.Name).err.log") `
                -PassThru
    $started += $app.Name
    Write-Host "[OK] $($app.Name) starting on port $($app.Port) (PID $($proc.Id)). Log: $stdOut" -ForegroundColor Green
}

Write-Host ""
Write-Host "Open http://localhost:8080 in your browser to use the app." -ForegroundColor Cyan
Write-Host "Logs are in: $logDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Started: $($started -join ', ')"