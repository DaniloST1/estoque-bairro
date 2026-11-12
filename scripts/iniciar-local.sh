#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
test -f .env || { echo 'Execute python3 scripts/configurar.py primeiro.'; exit 1; }
set -a
. ./.env
set +a
aps_jar=target/estoque-bairro-1.0.0.jar
if ! test -f "$aps_jar"; then
  if test -f distribuicao/estoque-bairro-1.0.0.jar; then
    aps_jar=distribuicao/estoque-bairro-1.0.0.jar
  else
    ./mvnw -DskipTests package
  fi
fi
exec java -jar "$aps_jar"
