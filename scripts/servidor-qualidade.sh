#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
test -n "${QUALITY_PASSWORD:-}" || { echo 'Defina QUALITY_PASSWORD para as contas temporárias.'; exit 1; }
./mvnw test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt -Dmdep.includeScope=test
exec java -cp "target/classes:target/test-classes:$(cat target/test-classpath.txt)" br.com.estoquebairro.QualityServer
