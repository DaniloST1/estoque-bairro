"""Restaura exclusivamente em banco NOVO estoque_restore_*. Não altera a origem."""
from pathlib import Path
from datetime import datetime
import subprocess,sys
root=Path(__file__).resolve().parent.parent
if len(sys.argv)!=2: raise SystemExit('Uso: python scripts/restaurar-teste.py backups/arquivo.dump')
source=Path(sys.argv[1]).resolve()
if not source.is_file(): raise SystemExit('Backup não encontrado.')
database='estoque_restore_'+datetime.now().strftime('%Y%m%d_%H%M%S')
base=['docker','compose','exec','-T','db']
subprocess.run(base+['createdb','-U','estoque',database],cwd=root,check=True)
with source.open('rb') as f:
    subprocess.run(base+['pg_restore','-U','estoque','-d',database,'--no-owner','--exit-on-error'],cwd=root,stdin=f,check=True)
subprocess.run(base+['psql','-U','estoque','-d',database,'-c','SELECT count(*) AS produtos FROM produto; SELECT count(*) AS movimentos FROM movimentacao;'],cwd=root,check=True)
with (root/'scripts/conferir-saldos.sql').open('rb') as f:
    subprocess.run(base+['psql','-U','estoque','-d',database,'-v','ON_ERROR_STOP=1'],cwd=root,stdin=f,check=True)
print('Banco de conferência:',database)
print('Compare também os totais com a origem. Mantenha o backup protegido.')
