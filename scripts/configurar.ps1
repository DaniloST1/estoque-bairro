$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$target = Join-Path $root '.env'
if (Test-Path $target) { throw '.env já existe. A configuração foi preservada.' }
function NovaSenha {
  $bytes = New-Object byte[] 24
  $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  $rng.GetBytes($bytes)
  $rng.Dispose()
  return ([BitConverter]::ToString($bytes)).Replace('-','').ToLower()
}
$lines = @("DB_PASSWORD=$(NovaSenha)", 'ADMIN_LOGIN=admin', "ADMIN_PASSWORD=$(NovaSenha)", "OPERATOR_PASSWORD=$(NovaSenha)", 'DEMO_DATA=true', 'PORT=8080')
[System.IO.File]::WriteAllLines($target, $lines, (New-Object System.Text.UTF8Encoding $false))
Write-Host 'Configuração criada. Consulte .env para as senhas de admin e operador.'
Write-Host 'Agora execute: docker compose up --build -d'
