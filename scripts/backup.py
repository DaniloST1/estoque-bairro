"""Backup via Docker Compose. Grava bytes sem corromper binário no PowerShell."""
from pathlib import Path
from datetime import datetime
import subprocess
root=Path(__file__).resolve().parent.parent
folder=root/'backups'; folder.mkdir(exist_ok=True)
target=folder/('estoque_'+datetime.now().strftime('%Y%m%d_%H%M%S')+'.dump')
with target.open('xb') as f:
    result=subprocess.run(['docker','compose','exec','-T','db','pg_dump','-U','estoque','-d','estoque_bairro','-Fc'],cwd=root,stdout=f)
if result.returncode:
    target.unlink(missing_ok=True)
    raise SystemExit('Backup falhou. Nenhum arquivo foi mantido.')
print('Backup criado:',target)
