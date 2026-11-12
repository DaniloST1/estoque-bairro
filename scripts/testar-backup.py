"""T24 no QualityServer: dump real e restauração em banco novo. Requer pg_dump/pg_restore em PATH."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,tempfile,time
import psycopg
root=Path(__file__).resolve().parent.parent
base='host=127.0.0.1 port=55432 user=postgres'
name='estoque_restore_'+str(int(time.time()))
pgdump=shutil.which('pg_dump');pgrestore=shutil.which('pg_restore')
if not pgdump or not pgrestore:raise SystemExit('Instale o cliente PostgreSQL e inclua seus binários no PATH.')
def snapshot(dbname):
    with psycopg.connect(base+' dbname='+dbname) as db:
        result={}
        for table in ['categoria','produto','usuario','movimentacao']:
            rows=db.execute('SELECT row_to_json(t)::text FROM '+table+' t ORDER BY id').fetchall()
            result[table]={'registros':len(rows),'sha256':hashlib.sha256('\n'.join(r[0] for r in rows).encode()).hexdigest()}
        result['divergencias_saldo']=len(db.execute((root/'scripts/conferir-saldos.sql').read_text()).fetchall())
        result['sequencias']={t:db.execute('SELECT last_value,is_called FROM '+t+'_id_seq').fetchone() for t in ['categoria','produto','usuario','movimentacao']}
        return result
with psycopg.connect(base+' dbname=postgres',autocommit=True) as db:
    assert db.execute('SELECT marcador FROM quality_environment').fetchone()[0]=='estoque-bairro-isolado'
    db.execute('CREATE DATABASE '+name)
before=snapshot('postgres')
with tempfile.TemporaryDirectory(prefix='backup-aps-') as folder:
    dump=Path(folder)/'backup.dump'
    subprocess.run([pgdump,'-h','127.0.0.1','-p','55432','-U','postgres','-d','postgres','-Fc','-f',str(dump)],check=True)
    size=dump.stat().st_size
    subprocess.run([pgrestore,'-h','127.0.0.1','-p','55432','-U','postgres','-d',name,'--no-owner','--exit-on-error',str(dump)],check=True)
    after=snapshot(name)
    result={'teste':'T24','data':time.strftime('%Y-%m-%dT%H:%M:%S%z'),'executor':'automação técnica','ferramenta':subprocess.check_output([pgdump,'--version'],text=True).strip(),'origem':'PostgreSQL temporário do QualityServer','destino':name,'bytes_dump':size,'antes':before,'depois':after,'resultado':'APROVADO' if before==after and after['divergencias_saldo']==0 else 'REPROVADO'}
(root/'evidencias/backup.json').write_text(json.dumps(result,indent=2,ensure_ascii=False))
print(result['resultado'],'- backup restaurado e comparado')
if result['resultado']!='APROVADO':raise SystemExit(1)
