# run.ps1 — Docker environment launcher (Windows PowerShell)
# ─────────────────────────────────────────────────────────────────────────────
# Analogous to Maven Profiles:
#   .\run.ps1 -Env infra   → infra only (postgres, redis, rabbitmq, maildev)
#   .\run.ps1 -Env dev     → like mvn -Pdev  (full dev stack)
#   .\run.ps1 -Env prod    → like mvn -Pprod (production stack)
#
# USAGE:
#   .\run.ps1 [-Env <dev|infra|prod>] [-Action <up|down|logs|ps|build>]
#
# EXAMPLES:
#   .\run.ps1                          # Start infra only (default)
#   .\run.ps1 -Env infra               # Infra only — run Spring from IDE
#   .\run.ps1 -Env dev                 # Full dev stack (Spring in Docker too)
#   .\run.ps1 -Env prod                # Production stack
#   .\run.ps1 -Env dev -Action down    # Stop dev stack
#   .\run.ps1 -Env dev -Action logs    # Follow backend logs
#   .\run.ps1 -Env prod -Action build  # Rebuild prod images
# ─────────────────────────────────────────────────────────────────────────────

param(
    [ValidateSet("dev", "infra", "prod")]
    [string]$Env = "infra",

    [ValidateSet("up", "down", "logs", "ps", "build", "pull", "restart")]
    [string]$Action = "up"
)

$BASE  = "docker-compose.base.yml"
$DEV   = "docker-compose.dev.yml"
$PROD  = "docker-compose.prod.yml"

# Full compose command prefixes
$DevCmd  = "docker compose -f $BASE -f $DEV"
$ProdCmd = "docker compose -f $BASE -f $PROD"

function Start-Infra {
    Write-Host "🚀 Starting dev infra (postgres, redis, rabbitmq, maildev)..." -ForegroundColor Cyan
    Write-Host "   → MailDev inbox : http://localhost:1080" -ForegroundColor Green
    Write-Host "   → RabbitMQ UI   : http://localhost:15672  (guest / guest)" -ForegroundColor Green
    Write-Host "   → Postgres      : localhost:5432" -ForegroundColor Green
    Write-Host "   → Redis         : localhost:6379" -ForegroundColor Green
    Write-Host ""
    Write-Host "   Run Spring from IntelliJ with profile = dev for hot-reload." -ForegroundColor Yellow
    Invoke-Expression "$DevCmd up -d postgres redis rabbitmq maildev"
}

switch ($Env) {
    "infra" {
        switch ($Action) {
            "up"   { Start-Infra }
            "down" { Invoke-Expression "$DevCmd down" }
            "ps"   { Invoke-Expression "$DevCmd ps" }
            "logs" { Invoke-Expression "$DevCmd logs -f postgres redis rabbitmq maildev" }
            default { Start-Infra }
        }
    }

    "dev" {
        switch ($Action) {
            "up" {
                Write-Host "🚀 Starting FULL dev stack (includes Spring backend in Docker)..." -ForegroundColor Cyan
                Write-Host "   → App      : http://localhost:8080" -ForegroundColor Green
                Write-Host "   → MailDev  : http://localhost:1080" -ForegroundColor Green
                Write-Host "   → RabbitMQ : http://localhost:15672" -ForegroundColor Green
                Invoke-Expression "$DevCmd up -d"
            }
            "down"    { Invoke-Expression "$DevCmd down" }
            "logs"    { Invoke-Expression "$DevCmd logs -f backend-app" }
            "ps"      { Invoke-Expression "$DevCmd ps" }
            "build"   { Invoke-Expression "$DevCmd build" }
            "restart" { Invoke-Expression "$DevCmd restart backend-app" }
            default   { Invoke-Expression "$DevCmd up -d" }
        }
    }

    "prod" {
        switch ($Action) {
            "up" {
                Write-Host "🚀 Starting PRODUCTION stack..." -ForegroundColor Yellow
                Invoke-Expression "$ProdCmd up -d"
            }
            "down"    { Invoke-Expression "$ProdCmd down" }
            "logs"    { Invoke-Expression "$ProdCmd logs -f backend-app" }
            "ps"      { Invoke-Expression "$ProdCmd ps" }
            "build"   { Invoke-Expression "$ProdCmd build" }
            "pull"    { Invoke-Expression "$ProdCmd pull" }
            "restart" { Invoke-Expression "$ProdCmd restart backend-app" }
            default   { Invoke-Expression "$ProdCmd up -d" }
        }
    }
}
