# Dashboard Administrativo — análise e projeto

## Contexto e solução

O painel foi acrescentado ao PediuPatrão **sem substituir** a aplicação de pedidos. Usa a sessão de autenticação, a coleção `pedidos` já existente e a estrutura de telas em Thymeleaf. A API de leitura entrega JSON para um módulo JavaScript que atualiza os indicadores, os gráficos e as tabelas. Não foi necessário criar entidades artificiais nem valores fixos de demonstração.

### Requisitos implementados

| Requisito | Solução | Componentes |
| --- | --- | --- |
| RF01 — Indicadores gerais | Faturamento, pedidos, finalizados, ticket médio, em andamento, cancelamentos e taxa | `/api/dashboard/resumo`, cards da página |
| RF02 — Análise de vendas | Série temporal com períodos diários, semanais ou mensais, inclusive intervalos sem venda | `/api/dashboard/vendas`, gráfico SVG |
| RF03 — Situação dos pedidos | Quantidade de cada status e distribuição visual | `/api/dashboard/situacao`, gráfico de rosca e legenda |
| RF04 — Produtos mais vendidos | Seis produtos por unidades, com faturamento atribuído e desconto proporcional | `/api/dashboard/produtos`, barras de ranking |
| RF05 — Filtros | Data inicial/final, status, modalidade (entrega ou retirada) e agrupamento | Query string dos endpoints, formulário |
| RF06 — Integração com API | Um endpoint consolidado permite atualizar todos os painéis em uma só chamada | `GET /api/dashboard`, `fetch()` autenticado |
| RF07 — Atualização e tratamento | Atualizar manualmente, atualizar a cada 60 s na aba visível; loading, vazio, erro, sessão expirada e conflito entre requisições | `dashboard.js`, tratamento HTTP padrão |

## Modelagem de dados já existente

- `Pedido`: `id`, `entradaEm`, `status`, `tipoAtendimento`, `subtotal`, `valorDesconto`, `total`, `itens`, `clienteNome` e os eventos de saída/finalização/cancelamento.
- `ItemPedido`: `produtoId`, `descricao`, `quantidade`, `precoUnitario` — fotografia do produto no momento da venda, preservando o histórico caso o catálogo mude.
- `Produto`: dados do catálogo operacional (não usado para recomputar preços de pedidos históricos).
- O serviço de dashboard **não grava** dados. Busca pedidos por intervalo de `entradaEm` usando índice MongoDB sobre esse campo, calcula estatísticas e retorna DTOs (sem telefone ou endereço).

```text
[Página /dashboard (Thymeleaf)]
            |
            | GET /api/dashboard?inicio=...&fim=...&...
            v
[DashboardController] -> [DashboardService] -> [PedidoRepository] -> [MongoDB: pedidos]
            |                     |
            |                     +-- agrega KPIs, status, séries, ranking e recentes
            v
     [JSON (DTOs)] -> [dashboard.js] -> [cards / SVG / tabela]
```

## Regras de negócio adotadas

1. **Faturamento:** soma de `total` dos pedidos cujo status **atual** seja `FINALIZADO`, com desconto já aplicado; cancelados e em andamento não entram. O projeto não possui cadastro de pagamentos nem estornos, portanto o indicador reflete *pedidos finalizados*, e não conciliação financeira.
2. **Data de referência:** `entradaEm` (data de criação do pedido), inclusive para vendas finalizadas. Não é uma série por data de pagamento ou `finalizadoEm`. O intervalo é inclusivo nas datas e respeita `America/Sao_Paulo`, convertido em `[início à meia-noite, dia seguinte à meia-noite)` para consultas ao MongoDB.
3. **Ticket médio:** faturamento / quantidade de pedidos finalizados, arredondado em duas casas decimais. Sem pedidos finalizados: R$ 0,00.
4. **Cancelamentos:** incluem os pedidos no estado atual `CANCELADO`. Taxa = cancelados / total de pedidos filtrados x 100. Pedidos em andamento são os que não estão `FINALIZADO` nem `CANCELADO`.
5. **Ranking de produtos:** somente itens de pedidos `FINALIZADO`. A receita do item é a fração proporcional do `total` do pedido, incluindo desconto. A última linha recebe a diferença residual de centavos, para somar exatamente o total. Ordenação: quantidade decrescente, receita decrescente e nome crescente.
6. **Status e modalidade:** aplicados à **mesma seleção** usada por todos os recursos. Filtrar status `CANCELADO`, por exemplo, produz vendas/faturamento zero, por definição.
7. **Datas:** por padrão, últimos 30 dias corridos, incluindo hoje. Aceita até 366 dias corridos, inclusive. Um intervalo maior ou com datas invertidas devolve 422; data malformada ou enum inválida devolve 400. `agrupamento` omitido: DIA até 46 dias, SEMANA até 181, MES acima disso.
8. **Privacidade/permissão:** ADMIN e GERENTE podem consultar dashboard e API; ATENDENTE não pode. Endpoint não retorna contato e endereço do cliente. Reutiliza segurança da aplicação; leitura GET não requer CSRF, mas exige cookie de sessão.
9. **Atualização:** leitura atual, sem cache obrigatório. JavaScript cancela consultas anteriores quando novo filtro é aplicado e conserva os dados anteriores caso uma nova consulta falhe. O refresh automático é pausado quando a aba está oculta.

## API: parâmetros e respostas

Todas as rotas abaixo aceitam a mesma query string, com valores opcionais:

| Parâmetro | Exemplo | Valores |
| --- | --- | --- |
| `inicio` | `2026-11-01` | data AAAA-MM-DD, inclusiva |
| `fim` | `2026-11-04` | data AAAA-MM-DD, inclusiva |
| `status` | `FINALIZADO` | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO` |
| `tipo` | `ENTREGA` | `ENTREGA`, `RETIRADA` |
| `agrupamento` | `DIA` | `DIA`, `SEMANA`, `MES` |

- `GET /api/dashboard`: objeto JSON `{ periodo, resumo, vendas, situacao, produtos, pedidos }` (recomendado para a tela).
- `GET /api/dashboard/resumo`: apenas `resumo`.
- `GET /api/dashboard/vendas`: somente lista `vendas`.
- `GET /api/dashboard/situacao`: somente lista `situacao`.
- `GET /api/dashboard/produtos`: somente lista `produtos`.
- `GET /api/dashboard/pedidos`: oito pedidos mais recentes, dentro dos filtros.

Exemplo de consulta autenticada: `GET /api/dashboard?inicio=2026-11-01&fim=2026-11-04&tipo=ENTREGA&agrupamento=DIA`.

O JSON contém números (inclusive valores de dinheiro representados por `BigDecimal`), datas ISO (`YYYY-MM-DD`) e instantes ISO UTC. Exemplo ilustrativo de **estrutura** (valores fictícios, não cadastrados no projeto):

```json
{
  "periodo":{"inicio":"2026-11-01","fim":"2026-11-04","fuso":"America/Sao_Paulo","agrupamento":"DIA","status":null,"tipo":"ENTREGA","atualizadoEm":"2026-11-04T14:00:00Z"},
  "resumo":{"faturamento":85.00,"pedidosFinalizados":2,"totalPedidos":3,"ticketMedio":42.50,"pedidosCancelados":1,"taxaCancelamento":33.33,"pedidosEmAndamento":0},
  "vendas":[{"periodo":"2026-11-01","faturamento":45.00,"pedidos":1}],
  "situacao":[{"status":"FINALIZADO","descricao":"Finalizado","quantidade":2}],
  "produtos":[{"produtoId":"produto1","nome":"Pizza","quantidade":2,"faturamento":45.00}],
  "pedidos":[{"id":"pedido1","numero":"PEDIDO","cliente":"Exemplo","entradaEm":"2026-11-01T15:00:00Z","tipo":"ENTREGA","status":"FINALIZADO","total":45.00}]
}
```

**Atenção:** o JSON real inclui todas as sete situações e todos os intervalos agregados (mesmo com zero). O exemplo foi encurtado.

## Testes e validação

**Unitários:** `DashboardServiceTest` verifica receita sem cancelados, descontos proporcionais, ranking, filtros, séries vazias, agrupamento semanal, intervalo e fuso padrão.

**Integração:** `FluxoCompletoIT` acrescenta testes da autorização ADMIN/GERENTE vs. ATENDENTE, validação dos filtros e uma venda real feita pelas rotas de pedidos. Esses testes só são habilitados com `MONGODB_TEST_URI` apontando para um banco exclusivo terminado em `_test` (replica set configurado).

Com Maven e Java 17 ou superior:

```bash
./mvnw test
# Se houver MongoDB de testes: 
MONGODB_TEST_URI='mongodb://localhost:27017/pediupatrao_test?replicaSet=rs0&directConnection=true' ./mvnw verify
```

Para validar na interface: faça login com ADMIN ou GERENTE, abra `/dashboard`, compare o total de pedidos com `/pedidos`, finalize uma venda pelo fluxo do gerente, atualize o painel, filtre por `FINALIZADO`, teste um período sem pedidos, um período invertido e uma conta ATENDENTE.

## Limites conhecidos

- As agregações atuais são feitas na camada de serviço depois de buscar pedidos indexados pela data; o período é limitado a 366 dias. Para volumes muito grandes, deve-se migrar os cálculos para pipelines `aggregate` do MongoDB com paginação/índices complementares.
- O ranking e o gráfico referem-se ao estado atual do pedido, e não a um histórico de estados ou pagamentos; o sistema original não registra eventos financeiros dedicados.
- A interface consulta a API; não cria dados fictícios nem adiciona pedidos de exemplo automaticamente.
