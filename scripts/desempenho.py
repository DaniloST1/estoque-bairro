"""Q02/Q03 e T21. Só funciona no QualityServer isolado, porta fixa 55432."""
import argparse,concurrent.futures,csv,html,json,math,os,platform,statistics,time,uuid
from pathlib import Path
from html.parser import HTMLParser
import requests,psycopg
BASE='http://127.0.0.1:8080'
ROOT=Path(__file__).resolve().parent.parent
class Inputs(HTMLParser):
    def __init__(self,text):
        super().__init__(); self.values={};self.feed(text)
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=='input' and a.get('name'):self.values[a['name']]=a.get('value','')
def connect():return psycopg.connect('host=127.0.0.1 port=55432 user=postgres dbname=postgres')
def baseline():
    with connect() as db:
        assert db.execute('SELECT marcador FROM quality_environment').fetchone()[0]=='estoque-bairro-isolado'
        db.execute('TRUNCATE movimentacao,produto,categoria RESTART IDENTITY CASCADE')
        db.execute("INSERT INTO categoria(nome,nome_normalizado) VALUES ('Alimentos','ALIMENTOS'),('Bebidas','BEBIDAS'),('Limpeza','LIMPEZA')")
        db.execute("INSERT INTO produto(sku,nome,categoria_id,custo_referencia,preco_venda,estoque_minimo,saldo) SELECT 'PERF-'||lpad(n::text,4,'0'),'Produto de carga '||n,1+(n%3),20,28,5,1000 FROM generate_series(1,1000) n")
        admin=db.execute("SELECT id FROM usuario WHERE login='admin'").fetchone()[0]
        db.execute("INSERT INTO movimentacao(produto_id,usuario_id,tipo,motivo,quantidade,custo_unitario,chave_requisicao,assinatura,saldo_apos) SELECT p,%s,'ENTRADA','COMPRA',100,20,md5(p::text||'-'||n::text)::uuid,repeat('0',64),n*100 FROM generate_series(1,1000) p CROSS JOIN generate_series(1,10) n",(admin,))
def login():
    s=requests.Session();s.trust_env=False
    r=s.get(BASE+'/login',timeout=10);r.raise_for_status();token=Inputs(r.text).values['_csrf']
    r=s.post(BASE+'/login',data={'username':'admin','password':os.environ['QUALITY_PASSWORD'],'_csrf':token},timeout=10)
    if r.status_code!=200 or '/login' in r.url:raise RuntimeError('Falha no login de qualidade')
    return s

def execute(seconds,round_id):
    sessions=[login() for _ in range(5)];end=time.monotonic()+seconds
    def worker(i):
        s=sessions[i];rows=[];n=0
        while time.monotonic()<end:
            kind='consulta' if n%10<7 else 'movimentacao';start=time.perf_counter();ok=True
            try:
                if kind=='consulta':
                    path='/' if n%10==0 else '/produtos?q=PERF-&pagina='+str(n%50)
                    r=s.get(BASE+path,timeout=10)
                    ok=r.status_code==200 and '/login' not in r.url
                else:
                    r=s.get(BASE+f'/movimentos/novo?produto={i+1}&motivo=COMPRA',timeout=10)
                    data=Inputs(r.text).values;data.update(quantidade='1',motivo='COMPRA',custo='20',justificativa='Carga de teste')
                    r=s.post(BASE+'/movimentos/revisar',data=data,timeout=10)
                    token=Inputs(r.text).values
                    if 'Confirmar movimentação' not in r.text:raise RuntimeError('Revisão não aprovada')
                    r=s.post(BASE+'/movimentos/confirmar',data={'chave':token['chave'],'_csrf':token['_csrf']},timeout=10)
                    ok=r.status_code==200 and 'Registro confirmado' in r.text
            except Exception:ok=False
            rows.append({'rodada':round_id,'usuario_virtual':i+1,'operacao':kind,'ms':round((time.perf_counter()-start)*1000,3),'sucesso':ok})
            n+=1;time.sleep(.05)
        return rows
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        result=[row for rows in pool.map(worker,range(5)) for row in rows]
    for s in sessions:s.close()
    return result

def stats(rows):
    result={}
    for kind in ('consulta','movimentacao'):
        samples=[r for r in rows if r['operacao']==kind];times=sorted(r['ms'] for r in samples);errors=sum(not r['sucesso'] for r in samples)
        result[kind]={'amostras':len(times),'mediana_ms':statistics.median(times),'p95_ms':times[math.ceil(.95*len(times))-1],'max_ms':max(times),'erros':errors,'erro_percentual':errors/len(times)*100}
    return result

def run():
    parser=argparse.ArgumentParser();parser.add_argument('--aquecimento',type=int,default=30);parser.add_argument('--segundos',type=int,default=120);parser.add_argument('--rodadas',type=int,default=3);args=parser.parse_args()
    out=ROOT/'evidencias'/'desempenho';out.mkdir(parents=True,exist_ok=True)
    env={'data':time.strftime('%Y-%m-%dT%H:%M:%S%z'),'sistema':platform.platform(),'cpu':platform.processor(),'nucleos':os.cpu_count(),'aquecimento_seg':args.aquecimento,'rodada_seg':args.segundos,'rodadas':args.rodadas,'usuarios':5,'mix_planejado':'70% consultas / 30% movimentações','produtos_iniciais':1000,'movimentos_iniciais':10000,'rede':'loopback 127.0.0.1','medicao':'Consulta = HTTP GET completo; movimentação = GET formulário + POST revisão + POST confirmação com redirecionamento. Pausa 50 ms entre operações.'}
    if Path('/proc/meminfo').exists():env['memoria']=Path('/proc/meminfo').read_text().splitlines()[0]
    with connect() as db:env['postgresql']=db.execute('SELECT version()').fetchone()[0]
    allrows=[];summary={};baseline();print('Aquecimento iniciado',flush=True);execute(args.aquecimento,0)
    for rnd in range(1,args.rodadas+1):
        baseline();print('Rodada',rnd,'iniciada',flush=True);rows=execute(args.segundos,rnd);allrows.extend(rows);summary[str(rnd)]=stats(rows)
        with connect() as db:
            divergences=db.execute((ROOT/'scripts/conferir-saldos.sql').read_text()).fetchall()
        summary[str(rnd)]['divergencias_saldo']=len(divergences)
        (out/'resultado.json').write_text(json.dumps({'ambiente':env,'rodadas':summary},indent=2,ensure_ascii=False))
        print('Rodada',rnd,'concluída:',summary[str(rnd)],flush=True)
    with (out/'amostras.csv').open('w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=allrows[0].keys());w.writeheader();w.writerows(allrows)
    print('Evidências em',out,flush=True)
if __name__=='__main__':run()
