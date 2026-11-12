$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!$env:QUALITY_PASSWORD) { throw 'Defina QUALITY_PASSWORD para as contas temporárias.' }
& ./mvnw.cmd test-compile dependency:build-classpath '-Dmdep.outputFile=target/test-classpath.txt' '-Dmdep.includeScope=test'
if ($LASTEXITCODE -ne 0) { throw 'Falha na preparação.' }
$cp='target/classes;target/test-classes;'+(Get-Content target/test-classpath.txt -Raw).Trim()
& java -cp $cp br.com.estoquebairro.QualityServer
