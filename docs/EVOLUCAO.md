# Evolução e retestes

## Construção técnica

A aplicação mantém o escopo do documento de requisitos. As transações centralizadas, o bloqueio da linha do produto, a assinatura de solicitações e a proteção do último administrador foram implementados antes da avaliação funcional.

Durante a preparação da avaliação, ocorreram impedimentos de ambiente, registrados nos logs locais de construção:

| Ocorrência | Efeito observado | Tratamento | Reteste |
|---|---|---|---|
| Runtime Java sem compilador JDK compatível | Maven não conseguia compilar para Java 21 | Compilação com JDK 21 completo | Build e suíte concluídos |
| Certificado do proxy do ambiente ausente no JDK de avaliação | Download de dependências falhou | Uso do repositório de certificados confiáveis do ambiente, sem desativar TLS | Dependências resolvidas |
| Socket Unix do PostgreSQL indisponível no ambiente | Banco temporário não iniciava | Banco de teste configurado com TCP local, sem socket Unix | PostgreSQL real iniciou |
| Mockito tentou anexar agente dinamicamente | Inicialização do executor de testes falhou | Mock maker por subclasses; a suíte usa serviços reais | 38 testes passaram |

Esses impedimentos não são defeitos de estoque nem medições de usuários. Não foram contabilizados como casos funcionais aprovados. A versão candidata inicial não completou a avaliação por esses bloqueios; os resultados técnicos referem-se à versão 1.0.0 após os ajustes.

## Melhorias implementadas em 11/11/2026

| Mudança | Motivo | Verificação |
|---|---|---|
| Modal de registro nas telas de entrada e saída, com filtro de produto, saldo, total estimado e validação de saldo no navegador | Antes era preciso ir ao catálogo para escolher o produto | 24 verificações no navegador, inclusive celular, sem erros de JavaScript ou de política de segurança |
| Página de relatórios (/relatorios) com períodos, gráficos mensais, indicadores e CSV | Acompanhar entradas, vendas e perdas ao longo do tempo | Total de vendas da página conferido com consulta SQL independente |
| Correção do defeito D01 (resposta 405 em vez da tela de acesso negado) | Encontrado em teste ponta a ponta independente | T14 e T25 retestados; 28 de 28 verificações aprovadas |

A suíte JUnit (`mvnw verify`) não foi executada após essas mudanças, porque o ambiente usado não tinha acesso ao repositório Maven. As consultas novas ficaram fora do pacote `service`, cuja cobertura é verificada pelo JaCoCo.

## Melhorias propostas

1. Aplicar T22 com três participantes e priorizar ajustes a partir das dificuldades observadas. Não há resultado humano inferido.
2. Acrescentar estorno auditável ou ajuste com motivo e autorização antes de uso comercial que exija correção de lançamentos. Está fora do escopo atual.
3. Para implantação externa, separar credenciais de migração e execução, adotar HTTPS e testar o procedimento de recuperação na infraestrutura escolhida.
4. Avaliar paginação no agrupamento de quantidades do painel caso o catálogo cresça além da carga acadêmica.

As alterações futuras devem preservar os testes críticos e registrar versão, motivação, evidência e reteste. O grupo pode denominar V1 e V2 suas próprias rodadas acadêmicas, desde que preserve resultados reais e identifique os arquivos correspondentes.
