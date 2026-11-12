# Plano de qualidade

Versão avaliada: 1.0.0. Referência de requisitos: `REQUISITOS.pdf`. O relatório de resultados acompanha as evidências, sem atribuir resultados aos integrantes da turma.

## Metas e método

| Aspecto | Meta | Evidência |
|---|---|---|
| Correção | 100% dos críticos e pelo menos 95% dos funcionais planejados | JUnit, inspeção no navegador e matriz T01–T25 |
| Integridade | Nenhum saldo negativo ou divergente | SQL de conciliação e testes de concorrência |
| Acesso | Bloquear todas as tentativas indevidas previstas | Testes de sessão, rotas ADMIN e CSRF |
| Desempenho | p95 consulta ≤ 2 s, movimentação ≤ 3 s, erros < 1% | Três rodadas de 120 s com cinco usuários |
| Usabilidade | 9 de 9 tarefas sem ajuda; mediana da saída ≤ 60 s | Avaliação com três participantes reais |
| Manutenção | Linhas ≥ 80%, ramos ≥ 70% em service | JaCoCo, sem exclusões de classes da camada |
| Recuperação | Dados e totais preservados na restauração | Dump restaurado em banco novo e comparação |

## Testes automatizados

`./mvnw clean verify` (Windows: `mvnw.cmd clean verify`). A suíte inicia PostgreSQL real temporário, usa as mesmas migrations da aplicação, executa requests com Spring MockMvc e consulta o banco. Ela não usa SQLite/H2 como substituto. Os testes de concorrência executam transações em threads distintas.

T18 instala um trigger que lança erro ao inserir movimentação. A falha acontece depois da alteração do saldo, dentro da mesma transação. O teste verifica o retorno do saldo e a ausência de histórico. O trigger existe apenas no banco temporário da suíte.

## Desempenho T21

Em terminal separado, defina uma senha temporária para QUALITY_PASSWORD, com pelo menos 12 caracteres. Execute `scripts/servidor-qualidade.sh` ou `scripts/servidor-qualidade.ps1`. Esse servidor abre HTTP em 127.0.0.1:8080 e PostgreSQL temporário em 127.0.0.1:55432. Pare a aplicação comum antes para liberar a porta 8080.

Instale as dependências em ambiente Python virtual:

```sh
python -m venv .venv
# Ative .venv no seu sistema
python -m pip install -r scripts/requirements-quality.txt
python scripts/desempenho.py
```

QUALITY_PASSWORD deve estar definida também no terminal do script. O banco isolado recebe um marcador de segurança. O script exige esse marcador antes de substituir a carga. Nunca aponte scripts de carga para o banco da loja.

O padrão é 1.000 produtos e 10.000 entradas iniciais, 30 s de aquecimento, três rodadas de 2 min e cinco sessões. Cada usuário repete sete consultas e três movimentações, com pausa de 50 ms. O corte temporal pode deixar o percentual observado ligeiramente diferente de 70/30, por encerrar ciclos incompletos. Antes de cada rodada, a mesma base é restaurada por reconstrução determinística dos dados, preservando as contas. Reconciliar o estoque ao final de cada rodada.

A consulta mede um GET HTTP completo. A movimentação mede o fluxo completo de GET do formulário, POST de revisão e POST de confirmação, incluindo redirecionamento. São registrados mediana, p95 (posição teto de 0,95 × N), máximo e erros por tipo e rodada. Erros de negócio inesperados na carga válida contam como falha. Falhas técnicas devem ser investigadas. Resultados locais não garantem desempenho na hospedagem.

## Usabilidade T22

Selecione três participantes que não tenham desenvolvido as telas. Explique o objetivo, sem ensinar a sequência de cliques durante a medição. Use contas de teste e dados fictícios. Execute em cópias equivalentes da base:

1. Localizar o produto indicado pelo avaliador.
2. Registrar entrada de cinco unidades, com custo informado.
3. Registrar saída por venda de duas unidades.

Registre início, fim, duração, conclusão, ajuda e dificuldade em `USABILIDADE.csv`. Não inclua identificação pessoal além de P1, P2 e P3. Interrompa a tarefa se o participante quiser. Nove tarefas e meta de 90% exigem nove sucessos. Calcule a mediana somente dos tempos da tarefa de saída e relate também falhas. A avaliação deve ocorrer com pessoas reais; automação de navegador não substitui esse resultado.

## Defeitos e melhorias

Registre ID, requisito, passos, esperado, observado, impacto, versão, correção e reteste. Crítico: perda de dados, estoque negativo ou acesso indevido. Alto: função essencial bloqueada. Médio: função com alternativa. Baixo: apresentação sem impedir a tarefa. Use o arquivo `DEFEITOS.csv` e preserve resultados anteriores ao corrigir. O histórico de desenvolvimento técnico está em `EVOLUCAO.md`.

## Registro e fechamento

Para cada teste: ID, versão, ambiente, executor, data, pré-condição, procedimento, esperado, obtido, situação e evidência. Uma execução bloqueada não conta como aprovada. Aprovação = aprovados / planejados × 100. Execução = executados / planejados × 100. Os números de casos automatizados e os 25 cenários T são contagens diferentes, pois um cenário pode ter vários casos.

Revise o relatório com os resultados humanos, atividades reais e dados do grupo antes da entrega. Não substitua essas pendências por valores estimados.
