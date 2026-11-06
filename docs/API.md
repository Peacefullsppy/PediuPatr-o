# Rotas da API

A API usa a mesma sessão das telas. Faça login pelo formulário `/login`. Operações de escrita exigem um token CSRF da sessão atual. Não há HTTP Basic ou JWT.

1. `GET /api/csrf` obtém token, nome do parâmetro e nome do cabeçalho.
2. `POST /login` envia `username`, `password` e o token no parâmetro retornado. Preserve o cookie da sessão.
3. Após autenticar, consulte `GET /api/csrf` novamente: o token é renovado no login.
4. Envie o token no cabeçalho retornado, normalmente `X-CSRF-TOKEN`, em POST, PUT e DELETE.

O script `scripts/verificar_permissoes.py` implementa essa sequência com a biblioteca padrão do Python.

| Método | Caminho | Uso |
| --- | --- | --- |
| GET | `/api/dashboard` | Visão administrativa consolidada (ADMIN/GERENTE) |
| GET | `/api/dashboard/resumo` | KPIs de vendas e pedidos |
| GET | `/api/dashboard/vendas` | Série temporal de vendas |
| GET | `/api/dashboard/situacao` | Contagem por status |
| GET | `/api/dashboard/produtos` | Ranking dos seis produtos mais vendidos |
| GET | `/api/dashboard/pedidos` | Oito pedidos recentes filtrados |
| GET | `/api/pedidos?pagina=0` | Consulta paginada |
| GET | `/api/pedidos/{id}` | Detalhe |
| POST | `/api/pedidos` | Registra pedido |
| PUT | `/api/pedidos/{id}/itens` | Substitui itens de pedido recebido |
| POST | `/api/pedidos/{id}/status` | Avança a etapa |
| POST | `/api/pedidos/{id}/desconto` | Substitui o desconto |
| POST | `/api/pedidos/{id}/cancelamento` | Cancela com justificativa |
| GET, POST | `/api/clientes` | Consulta ou cadastra |
| GET, PUT, DELETE | `/api/clientes/{id}` | Consulta, atualiza ou exclui logicamente |
| GET, POST | `/api/produtos` | Consulta ou cadastra |
| PUT | `/api/produtos/{id}` | Atualiza nome, preço e situação |
| GET, POST | `/api/usuarios` | Consulta ou cadastra contas |
| PUT | `/api/usuarios/{id}/perfil` | Altera perfil e situação |
| GET | `/api/auditoria?entidade=PEDIDO&entidadeId={id}&pagina=0` | Consulta com filtros opcionais |

As permissões de cada operação estão na tabela do README. A API de auditoria não aceita escrita.

Pedido:

```json
{"clienteId":"ID_DO_CLIENTE","tipoAtendimento":"ENTREGA","itens":[{"produtoId":"ID_DO_PRODUTO","quantidade":2}]}
```

Itens:

```json
{"itens":[{"produtoId":"ID_DO_PRODUTO","quantidade":3}]}
```

Status, desconto e cancelamento usam corpos separados:

```json
{"status":"EM_PREPARACAO"}
```

```json
{"percentual":10}
```

```json
{"justificativa":"Cliente desistiu antes do preparo."}
```

Cliente:

```json
{"nome":"Ana Teste","telefone":"11987654321","endereco":{"cep":"08710000","logradouro":"Rua das Flores","numero":"12","complemento":"Casa","bairro":"Centro","cidade":"Mogi das Cruzes"}}
```

Produto:

```json
{"nome":"Calabresa","preco":50.00,"ativo":true}
```

Usuário (troque a senha de exemplo):

```json
{"login":"gerente","nome":"Gerência","senha":"troque-esta-senha","perfil":"GERENTE"}
```

Perfil:

```json
{"perfil":"ATENDENTE","ativo":true}
```

Somente os campos previstos são aceitos. A resposta não inclui senhas ou hashes. Os IDs são gerados pelo MongoDB.

## Códigos HTTP

- `200`: consulta ou alteração concluída.
- `201`: registro criado.
- `204`: cliente excluído logicamente.
- `400`: JSON, campo inesperado ou validação inválida.
- `401`: sem sessão autenticada em uma rota de API.
- `403`: permissão insuficiente ou token CSRF ausente/inválido.
- `404`: registro não encontrado.
- `409`: conflito de versão ou identificador duplicado.
- `422`: regra de negócio violada, como pular uma etapa.
- `503`: falha de acesso ao banco ou gravação que não pôde ser concluída. Recarregue os dados antes de repetir.

Para filtros (`inicio`, `fim`, `status`, `tipo`, `agrupamento`), regras de cálculo e exemplo de resposta, veja [DASHBOARD.md](DASHBOARD.md). Todas as rotas do dashboard são GET e exigem perfil ADMIN ou GERENTE.
