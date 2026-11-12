# Matriz de atendimento aos requisitos

Documento base: `REQUISITOS.pdf`. Os itens abaixo distinguem implementação técnica de obrigações acadêmicas que exigem atuação do grupo.

| Requisito | Implementação ou entrega | Verificação |
|---|---|---|
| RF01 | SecurityConfig, SessionUser, login e logout | T01, T02 e testes de revogação |
| RF02 | InventoryService.category e tela Categorias | T03, T04, category_edit |
| RF03 | Produto, cadastro e restrições SQL | T03–T05 |
| RF04 | Busca literal por nome/SKU, filtros, 20 itens por página | T11 |
| RF05 | Entrada COMPRA com custo e autor | T06 |
| RF06 | Saídas VENDA/PERDA, justificativa e saldo | T07–T09 |
| RF07 | Histórico paginado e filtro com fim inclusivo | T12 e renderização |
| RF08 | Painel, totais e unidades por produto | T10, T11 |
| RF09 | Edição administrativa e inativação com saldo zero | T13 |
| RF10 | Cadastro/edição de contas e bloqueio do último ADMIN | T14, T15 e concorrência administrativa |
| RN01–RN04 | Normalização, inteiros, BigDecimal, status e limites | T03–T09, validações e restrições |
| RN05 | Transação entre saldo e histórico | T18 |
| RN06 | SELECT FOR UPDATE no produto | T16 |
| RN07 | UUID, bloqueio da chave, assinatura do conteúdo e UNIQUE | T17 e reenvio concorrente |
| RN08 | Histórico sem edição/exclusão, trigger, inativação | T13, immutable_database_history |
| RN09 | Último custo e valores preservados na movimentação | T06, T19 |
| RN10 | TIMESTAMPTZ e America/Sao_Paulo | T12 |
| BD01–BD06 | Migration Flyway e índices | Restrições, concorrência e conciliação |
| UI01–UI03 | Português, moeda, foco, rótulos, erros e revisão | Renderização automatizada e inspeção no navegador; avaliação humana ainda pendente |
| RT01–RT03 | Separação de camadas, Maven/JDK fixados, Security/JDBC | Build verify e testes de acesso/CSRF/HTML |
| RT04 | Bootstrap de 3 categorias, 10 produtos e 2 contas | Inicialização da demonstração; senhas geradas localmente |
| RT05 | PostgreSQL local e alternativa Docker | Testes reais em PostgreSQL; Docker fornecido, sem afirmar execução neste ambiente |
| Q01–Q03 | Plano, scripts de carga e evidências versionadas | `PLANO_QUALIDADE.md` e `evidencias/` |
| Q04 / T22 | Roteiro e formulário de 9 tarefas | Pendente de três participantes reais |
| Q05 | Relatórios JUnit, cobertura e formulários | `evidencias/` e `DEFEITOS.csv` |
| Q06 | Registro de problemas técnicos, correções e retestes | `EVOLUCAO.md`; melhorias de usabilidade dependem dos participantes |
| Q07 | Falha transacional e restauração separada | T18 e evidência T24 |
| A01–A03 | Sistema justificado, requisitos e plano | Relatório e documentação |
| A04–A06 | Testes técnicos, resultados e melhorias | Evidências disponíveis; completar T22 |
| A07–A08 | Relatório técnico de apoio, referências e limites | Revisar redação, originalidade, normalização e reflexão do grupo |
| A09 / D10 | Ficha individual para preenchimento | Nomes, RA, atividades, horas e assinaturas reais pendentes |
| A10 / D11 | Apresentação editável e roteiro de demonstração | Apresentação oral e envio institucional dependem do grupo |
| D01–D06 | Capa com campos, resumo, sumário automático, introdução, teoria e método | Completar identificação e atualizar sumário no Word |
| D07–D09 | Arquitetura, banco, código integral, resultados, conclusões e referências | Fonte completa em src; atualizar conclusões após T22 |
| F01–F03 | Sistema, build, testes e arquivos de evidência | Consultar RESULTADOS.md para métricas observadas |
| F04 | Material acadêmico de apoio | Não declarar concluído antes das atividades humanas e revisão final |

## Limites da validação

Os cenários de interface têm inspeção técnica e testes automatizados. Isso não comprova a taxa de sucesso de usuários humanos. O Dockerfile e Compose são caminhos de distribuição, mas a validação executada usa a aplicação Java e PostgreSQL em processos locais. A instalação no computador da turma deve ser conferida antes da apresentação.
