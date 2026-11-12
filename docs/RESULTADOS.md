# Resultados da avaliação técnica

Versão: 1.0.0. Consolidação: 10/11/2026. Executor: automação técnica do assistente. Estes registros não atribuem testes ou atividades aos alunos.

## Resultado geral

- `mvn clean verify`: BUILD SUCCESS.
- JUnit: 38 testes executados, 38 aprovados, zero falhas, zero erros e zero ignorados.
- Camada service: 134 linhas cobertas de 137, 97.81%.
- Ramos da mesma camada: 98 de 100, 98,00%. Nenhuma classe de serviço excluída.
- Navegador: entrada, venda, perda, histórico, login/logout, tela móvel e bloqueio de operador aprovados.
- Backup: dump PostgreSQL restaurado em banco novo, contagens, hashes de registros e sequências iguais; nenhuma divergência de saldo.

T01 a T20, T21, T23, T24 e T25 possuem verificação técnica: 24 dos 25 cenários planejados. T22 permanece **PENDENTE**. Isso corresponde a 96% dos cenários T com verificação concluída, e não a 100% de conclusão da APS. Os 15 cenários T01–T15 e os 15 IDs classificados como críticos no PDF têm evidência automatizada. A contagem de 38 casos JUnit é diferente da contagem de cenários T, pois há casos adicionais e parametrizados.

## Desempenho T21

Base inicial em cada rodada: 1.000 produtos e 10.000 movimentações. Cinco sessões, 30 s de aquecimento e três rodadas de 120 s. 30626 operações medidas. Os ciclos têm 7 consultas e 3 movimentações, com pausa de 50 ms. O encerramento temporal pode interromper ciclos, de modo que a proporção observada se aproxima de 70/30.

| Rodada | Operação | Amostras | Mediana ms | p95 ms | Máximo ms | Erros |
|---|---|---:|---:|---:|---:|---:|
| 1 | consulta | 7204 | 5.217 | 19.426 | 50.181 | 0 |
| 1 | movimentacao | 3077 | 9.537 | 15.212 | 34.762 | 0 |
| 2 | consulta | 7153 | 5.467 | 20.403 | 51.983 | 0 |
| 2 | movimentacao | 3055 | 9.893 | 16.730 | 31.051 | 0 |
| 3 | consulta | 7100 | 5.567 | 20.987 | 61.148 | 0 |
| 3 | movimentacao | 3037 | 9.573 | 18.491 | 46.676 | 0 |

As três rodadas ficaram abaixo das metas de 2.000 ms para consultas e 3.000 ms para movimentações, sem erros e sem divergências de saldo. A movimentação mede o fluxo HTTP completo, incluindo formulário, revisão, confirmação e redirecionamento.

O ambiente é compartilhado, com rede loopback. Outros trabalhos técnicos ocorreram durante a medição. Os resultados não garantem o comportamento em rede externa nem em outra máquina. Veja `evidencias/desempenho/resultado.json` para ambiente e `amostras.csv` para os tempos individuais.

## Evidências

| Arquivo | O que demonstra |
|---|---|
| `evidencias/junit.xml` e `junit.txt` | Casos, duração e situação real |
| `evidencias/maven-verify.txt` | Compilação, execução e verificação da cobertura |
| `evidencias/cobertura/index.html` | Linhas, ramos e classes não cobertas |
| `evidencias/navegador.json` | Fluxos HTTP/UI, ausência de erros JS e acesso 403 |
| `evidencias/desempenho/` | Rodadas de carga e amostras |
| `evidencias/backup.json` | Comparação antes e depois da restauração |

## O que ainda depende do grupo

Aplicar T22 com três participantes reais, completar nomes/RA/professor e fichas individuais, revisar o relatório segundo a orientação da turma, atualizar o sumário e apresentar o sistema. O Docker Compose foi fornecido e revisado, mas não executado neste ambiente. Não houve publicação em serviço externo.

Não é possível afirmar atendimento integral às obrigações acadêmicas antes dessas atividades. O pacote contém o sistema implementado, evidências técnicas e instrumentos para concluir a parte humana.
