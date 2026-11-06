# Roteiro da demonstração

Grave a tela com o sistema em execução. Use contas e cadastros de teste e mantenha senhas fora da gravação. Reserve cerca de 6 a 8 minutos, ajustando ao limite informado pelo professor.

## Preparação

Inicie o sistema. Com o ADMIN, crie os usuários `gerente` e `atendente`, cada um com seu perfil. Cadastre Calabresa por R$ 50,00. Deixe terminais preparados para executar o script de permissões. Separe três pedidos: um de entrega, um de retirada e um que será cancelado.

## 1 Autenticação e permissões

Entre como ADMIN, mostre a tela de usuários e o perfil atribuído a cada conta. Saia, entre como ATENDENTE e cadastre uma cliente fictícia, com Rua das Flores e telefone de teste.

Registre duas pizzas Calabresa: o total deve ser R$ 100,00, sem um campo editável de total. Abra o detalhe e mostre o usuário e o horário de entrada.

Copie o ID completo do pedido pela URL. Execute `python scripts/verificar_permissoes.py`, selecione o login do ATENDENTE e informe esse ID. Mostre as tentativas de desconto, cancelamento e auditoria retornando 403 com um token CSRF válido. Isso demonstra o bloqueio no backend.

Repita o script autenticado como ADMIN. As operações de desconto e cancelamento também devem retornar 403; a consulta da auditoria deve retornar 200.

## 2 Gestão pelo gerente

Entre como GERENTE. No pedido de R$ 100,00, aplique 10% e mostre o total de R$ 90,00. Avance de Recebido para Em preparação e depois Pronto.

No script, entre como GERENTE e use o pedido ainda antes da saída. Mostre: desconto de 25% recusado, salto direto para Finalizado recusado e tentativa sem CSRF bloqueada. O script não aplica um desconto válido quando executado pelo gerente.

Registre a saída para entrega. Mostre data/hora e responsável e finalize. No outro pedido, demonstre a opção Retirado e depois Finalizado.

## 3 Cancelamento

Abra o terceiro pedido, ainda antes da saída, e cancele com o motivo “Cliente desistiu antes do preparo”. Mostre que itens, valores e data de entrada continuam disponíveis. Tente alterar o status pela API: deve ser recusado. Use o teste de integração ou uma requisição com o token da sessão para mostrar esse bloqueio.

## 4 Alteração de cliente e auditoria

Entre como ATENDENTE. Troque Rua das Flores por Av. Brasil e altere o telefone. Entre novamente como GERENTE e abra a auditoria.

Mostre os eventos do pedido: criação, desconto, transições, saída e cancelamento no pedido correspondente. Confira usuário, perfil, data/hora, valores anteriores e novos e justificativa.

Filtre a entidade CLIENTE e mostre o endereço e o telefone anteriores e novos. Explique que a consulta é exclusiva de ADMIN e GERENTE e que não existem operações comuns para editar ou apagar eventos.

## 5 Código e execução dos testes

Mostre `SecurityConfig` e a anotação `@PreAuthorize` nas operações exclusivas do gerente em `PedidoService`. Mostre o cálculo no servidor, as transições e `@Transactional`. Abra `AuditoriaRepository` para mostrar que só há inserção e consulta.

Execute os testes e mostre o resultado real. Os testes de integração só executam com `MONGODB_TEST_URI` configurada. Finalize mostrando o README e o repositório individual, depois de publicá-lo.
