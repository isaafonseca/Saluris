# =============================================================================
# SALURIS IA - Script de inicialização completa (Backend + PostgreSQL + Frontend)
# =============================================================================
$ErrorActionPreference = "Continue"
$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "   SALURIS IA - Gestão Inteligente de Documentos e Prazos SST" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# Encerrar instâncias anteriores do Backend (:8080) e Frontend (:5173) rapidamente via netstat
$lines = netstat -ano | Select-String "LISTENING"
foreach ($line in $lines) {
    $text = $line.ToString()
    if ($text -match ":(8080|5173)\s+.*LISTENING\s+(\d+)$") {
        $pidToKill = [int]$Matches[2]
        if ($pidToKill -gt 0) {
            Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "Iniciando o Servidor MVC Spring Boot (porta 8080, perfil 'local' com PostgreSQL embarcado)"
Write-Host "e a camada de Views React + Vite (porta 5173)..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Contas de demonstração pré-cadastradas (senha padrão: Saluris@2026!):" -ForegroundColor Green
Write-Host "  • Administrador (T.I.) : admin@saluris.com.br"
Write-Host "  • Especialista SST     : especialista@saluris.com.br"
Write-Host "  • Médico do Trabalho   : medico@saluris.com.br"
Write-Host "  • RH / Empresa         : rh@saluris.com.br"
Write-Host ""

$mvnCmd = if (Test-Path "C:\apache-maven-3.9.11\bin\mvn.cmd") { "C:\apache-maven-3.9.11\bin\mvn.cmd" } else { "mvn" }

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$rootDir'; & '$mvnCmd' spring-boot:run '-Dspring-boot.run.profiles=local'"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$rootDir'; npm run dev"

Write-Host "Aguardando o Backend (porta 8080) concluir a inicialização..." -ForegroundColor Cyan
for ($i = 1; $i -le 30; $i++) {
    Start-Sleep -Seconds 1
    $tcp = New-Object System.Net.Sockets.TcpClient
    try {
        $tcp.Connect("127.0.0.1", 8080)
        if ($tcp.Connected) {
            $tcp.Close()
            Write-Host "Backend ativo na porta 8080 e conectado ao PostgreSQL (porta 54329)!" -ForegroundColor Green
            break
        }
    } catch {
        # Ainda iniciando
    } finally {
        $tcp.Dispose()
    }
}

Write-Host "Acesse no navegador: http://localhost:5173" -ForegroundColor Cyan
Start-Process "http://localhost:5173"
