"""Gera credenciais locais. Não sobrescreve configuração existente."""
from pathlib import Path
import secrets
root = Path(__file__).resolve().parent.parent
target = root / '.env'
if target.exists():
    raise SystemExit('.env já existe. A configuração foi preservada.')
values = {'DB_PASSWORD': secrets.token_hex(20), 'ADMIN_LOGIN': 'admin',
          'ADMIN_PASSWORD': secrets.token_urlsafe(18), 'OPERATOR_PASSWORD': secrets.token_urlsafe(18),
          'DEMO_DATA': 'true', 'PORT': '8080'}
with target.open('x', encoding='utf-8') as f:
    f.write('\n'.join(f'{k}={v}' for k,v in values.items())+'\n')
target.chmod(0o600)
print('Configuração criada. Consulte .env para as senhas de admin e operador.')
print('Agora execute: docker compose up --build -d')
