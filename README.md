# Estoque Bairro

Sistema de estoque para a APS de Ciência da Computação da UNIP. Java 21, Spring Boot 3.5.16, Spring MVC, Thymeleaf, JDBC e PostgreSQL. Versão 1.0.0.

> **JAR pronto:** baixe `estoque-bairro-1.0.0.jar` na página [Releases](../../releases) deste repositório e coloque-o na pasta `distribuicao/`. Ele só é necessário para a execução sem Docker; o modo Docker compila a partir do código-fonte.

## Comece por aqui

1. Extraia a pasta ZIP inteira. Não execute os arquivos dentro do ZIP.
2. No Windows, instale e abra Docker Desktop com suporte a contêineres Linux. Abra o PowerShell na pasta `estoque-bairro`.
3. Gere as senhas locais:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\configurar.ps1
```

4. Inicie o sistema e o banco:

```powershell
docker compose up --build -d
```

5. Espere a inicialização. A primeira execução baixa dependências e pode levar alguns minutos.

```powershell
docker compose logs -f app
```

6. Acesse **http://localhost:8080**. Consulte o arquivo `.env` local para as senhas. Login de administrador: `admin`. Login de operador da demonstração: `operador`.

As credenciais são geradas no seu computador. O ZIP não contém senhas reais. A carga inicial cria três categorias e dez produtos com saldo zero. Registre as entradas antes das vendas. O bootstrap só roda quando não há nenhuma conta no banco.

No Linux/macOS, gere o `.env` com `python3 scripts/configurar.py` e use o mesmo comando Docker Compose. Não é necessário instalar Java ou PostgreSQL no host para esse modo.

**Parar:** `docker compose stop`. **Retomar:** `docker compose up -d`. Os dados permanecem no volume `estoque_data`. Não use `docker compose down -v` se quiser preservá-los.

## Execução sem Docker

Instale JDK 21 completo (incluindo javac) e PostgreSQL 14 ou posterior. A suíte foi preparada para PostgreSQL real, não H2. Crie um usuário e um banco exclusivos:

```sql
CREATE ROLE estoque LOGIN;
-- No psql, use \password estoque para definir a senha sem gravá-la em um script.
CREATE DATABASE estoque_bairro OWNER estoque;
```

Execute a configuração do `.env`, substitua `DB_PASSWORD` pela senha escolhida e acrescente:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/estoque_bairro
DB_USER=estoque
```

No Windows: `powershell -ExecutionPolicy Bypass -File .\scripts\iniciar-local.ps1`.
No Linux/macOS: `sh scripts/iniciar-local.sh`.
O Maven Wrapper baixa Maven 3.9.11. A primeira compilação precisa de internet. Para desenvolvimento, abra `pom.xml` no IntelliJ IDEA ou Eclipse e configure JDK 21.

Os scripts locais usam primeiro o JAR em `target/`, se houver compilação própria, ou o JAR pronto em `distribuicao/`. O JAR contém as dependências da aplicação; Java e o servidor PostgreSQL continuam necessários. Depois de alterar o código, compile novamente com `mvnw.cmd clean verify` para usar as alterações.

## O que funciona

- Login e logout, administrador e operador, sessão de 30 minutos e revogação de sessões após mudança de acesso.
- Categorias e produtos com SKU único, pesquisa, filtros e paginação.
- Entrada por compra, saída por venda e perda justificada. Resumo antes da confirmação.
- Transação atômica, bloqueio do produto, proteção contra reenvio e saldo negativo.
- Histórico imutável, valores preservados e datas no fuso de São Paulo.
- Painel de reposição, valor estimado e totais por período e por produto.
- Telas de entrada e saída com resumo do mês, últimas movimentações e modal de registro (produto, quantidade, motivo, valor e justificativa), sempre seguido da tela de revisão.
- Relatórios com períodos de 3, 6 e 12 meses, todo o período ou datas livres: valores e unidades por mês em gráficos, comparação com o período anterior, ticket médio, taxa de perda, produtos mais vendidos, perdas, categorias, responsáveis e exportação CSV.
- Administração de contas, proteção do último administrador, SQL parametrizado e CSRF.

## Organização

| Pasta | Conteúdo |
|---|---|
| `src/main/java/.../model` | Modelos e comando de movimentação |
| `repository` | SQL parametrizado e acesso ao banco |
| `service` | Regras e fronteiras transacionais |
| `web` | Controllers, formulários e formatação |
| `config` | Autenticação, sessão e carga inicial |
| `src/main/resources/templates` | Telas Thymeleaf |
| `src/main/resources/db/migration` | Estrutura versionada pelo Flyway |
| `src/test` | Testes JUnit e servidor isolado de avaliação |
| `scripts` | Inicialização, backup, restauração e carga |
| `docs` | Requisitos, arquitetura, relatório e roteiro da APS |
| `evidencias` | Resultados efetivamente executados |

## Testes e cobertura

```powershell
.\mvnw.cmd clean verify
```

No Linux/macOS: `./mvnw clean verify`. Requer JDK 21, internet na primeira execução e permissão para iniciar processos locais. O teste inicia um PostgreSQL temporário em porta livre e elimina seus dados ao encerrar. No Linux, execute como usuário comum, não root. O banco da loja não é utilizado.

Os relatórios ficam em `target/surefire-reports` e `target/site/jacoco/index.html`. A compilação de verificação falha se a camada `service` ficar abaixo de 80% de linhas ou 70% de ramos. `-DskipTests package`, usado na imagem Docker, só compila; não substitui `verify`.

Para carga de desempenho, siga `docs/PLANO_QUALIDADE.md`. Para o teste com três participantes, preencha `docs/USABILIDADE.csv`. Resultados humanos e assinaturas não são preenchidos automaticamente.

## Backup e recuperação

Com Docker iniciado e Python 3 instalado:

```powershell
python scripts/backup.py
python scripts/restaurar-teste.py backups/ARQUIVO.dump
```

O segundo comando cria um banco separado `estoque_restore_...`, sem sobrescrever a loja. Compare contagens, valores e a consulta `scripts/conferir-saldos.sql`. Guarde backups fora do computador da demonstração e proteja-os, pois contêm contas e histórico.

## Limites e entrega acadêmica

Uma loja, produtos em unidades inteiras. Não inclui caixa financeiro, nota fiscal, clientes, fornecedores, estorno, ajustes de inventário, lotes ou validade. A estimativa usa o último custo de entrada. Venda registrada não representa recebimento nem lucro contábil.

A implementação e as evidências técnicas não substituem a participação do grupo. Revise o relatório, preencha nomes/RA/professor, aplique as nove tarefas com três participantes reais, registre atividades de cada aluno e realize a apresentação. Consulte `docs/MATRIZ_REQUISITOS.md` para a situação de cada requisito.

O modo padrão publica somente no próprio computador (`127.0.0.1`). Para acesso externo, configure HTTPS, `COOKIE_SECURE=true`, credenciais próprias, backups e revisão da infraestrutura. Não foi realizada publicação na Hostinger ou Supabase. Este projeto atende o escopo da APS e não implementa os servidores específicos do trabalho de DSD.
