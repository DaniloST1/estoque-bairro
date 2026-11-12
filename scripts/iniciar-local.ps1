$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!(Test-Path '.env')) { throw 'Execute scripts/configurar.ps1 primeiro.' }
Get-Content '.env' | ForEach-Object {
  if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1],$matches[2],'Process') }
}
$apsJar='target/estoque-bairro-1.0.0.jar'
if (!(Test-Path $apsJar)) {
  if (Test-Path 'distribuicao/estoque-bairro-1.0.0.jar') {
    $apsJar='distribuicao/estoque-bairro-1.0.0.jar'
  } else {
    & ./mvnw.cmd -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }
  }
}
& java -jar $apsJar
