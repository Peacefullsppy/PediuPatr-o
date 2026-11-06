# Validação da implementação

Verificação realizada em 15 de outubro de 2026 com Java 17.0.20, Maven 3.9.11, Spring Boot 3.5.16 e MongoDB 7.0.16 em replica set de um nó.

## Testes automatizados

A execução de `mvn verify`, com `MONGODB_TEST_URI` apontando para um banco de teste separado, terminou com `BUILD SUCCESS`.

| Conjunto | Executados | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| `PedidoServiceTest` | 22 | 0 | 0 | 0 |
| `FluxoCompletoIT` | 23 | 0 | 0 | 0 |
| Total | 45 | 0 | 0 | 0 |

Os testes unitários verificam cálculos, arredondamento, substituição do desconto, limites, cancelamento e o conjunto de transições possíveis para entrega e retirada. A parametrização percorre todas as combinações de origem/destino e modalidade.

A integração executa as rotas com Spring Security e MongoDB reais. Foram verificados: login dos três perfis, credenciais inválidas, CSRF, acesso direto aos serviços, restrições do ADMIN/ATENDENTE, manipulação do total, quantidades fracionárias, desconto, entrada/saída, cancelamento, correção de itens, cadastros, atualização de endereço/telefone, auditoria e bloqueio de escrita/exclusão de eventos.

Dois testes provocam uma falha na inserção da auditoria e conferem no banco que a alteração do cliente e a inclusão do pedido foram revertidas. Outros verificam que alterações de perfil e desativação passam a valer em sessões abertas e que mudanças de preço no cardápio não reescrevem um pedido existente.

## Aplicação em execução

O JAR foi iniciado com um banco separado e acessado por navegador automatizado. O percurso incluiu:

- Login como ADMIN, criação das contas de GERENTE/ATENDENTE e cadastro de produto.
- Login como ATENDENTE, cadastro de cliente e criação de pedido de duas pizzas a R$ 50,00.
- Execução do script de permissões com sessões reais dos três perfis.
- Login como GERENTE, aplicação de 10% de desconto e conferência do total de R$ 90,00.
- Transições de entrega até FINALIZADO e conferência dos seis eventos do pedido na auditoria.
- Inspeção visual da tela de pedido em desktop e celular e da consulta de auditoria.

As tentativas não autorizadas retornaram 403. O desconto de 25% foi recusado com 400, o salto de etapa com 422 e a tentativa sem CSRF com 403. Não ocorreram erros de JavaScript no percurso. A tabela de itens possui rolagem interna quando necessário em telas pequenas.

## Limites da verificação

O Docker Compose foi revisado e seu YAML validado, mas não foi executado neste ambiente, que não possui Docker. O servidor, o banco real, a compilação, os testes e o navegador foram executados diretamente.

Não houve comparação ou integração com o projeto inicial, pois seus arquivos não foram anexados. O repositório GitHub e o vídeo individual ainda precisam ser providenciados pelo aluno. Nenhuma dessas duas etapas foi simulada como concluída.
