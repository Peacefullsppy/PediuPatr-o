# LINK DO VÍDEO PARA O YOUTUBE: https://www.youtube.com/watch?v=fRBXTED3sDY


# PediuPatrão

Sistema de uma pizzaria para registrar clientes, manter o cardápio e acompanhar pedidos. A atividade foi implementada em Java 17, Spring Boot 3.5.16, Spring Security, Thymeleaf e MongoDB.

## Sobre esta entrega

O arquivo `Atividade_Pratica_M1_Parte2(1)(1).docx` contém o enunciado, mas não inclui o projeto inicial mencionado nele. Por isso, este código é uma implementação independente, organizada nas camadas solicitadas. Não foi possível conferir nomes de pacotes, telas ou funcionalidades que só existam no projeto original. Se o professor exigir a evolução daquele repositório específico, será necessário integrar estas mudanças à base fornecida por ele.

O projeto inclui código, formulários, testes e um roteiro para a demonstração. A publicação no seu GitHub individual e a gravação do vídeo ainda precisam ser realizadas. O roteiro não substitui o vídeo.

## Dashboard Administrativo (atividade complementar)

Esta versão evolui o sistema de pedidos original com **Dashboard Administrativo** (RF01–RF07) integrado à mesma aplicação. O painel está em **`/dashboard`**, visível no menu apenas para **ADMIN** e **GERENTE**. Não foi criado um segundo frontend nem um banco novo.

- Indicadores: faturamento de pedidos finalizados, volume de pedidos, ticket médio, andamento e cancelamento.
- Gráficos: evolução das vendas (dia, semana ou mês), situação atual dos pedidos e ranking dos produtos mais vendidos.
- Consultas: período, status e modalidade (entrega/retirada), com atualização manual e automática a cada 60 segundos.
- Integração: `GET /api/dashboard` retorna todos os blocos de uma vez. Endpoints individuais permitem consultar resumo, vendas, situações, produtos e pedidos.
- Sem dados de demonstração inventados: quando não existem pedidos, os indicadores e gráficos refletem **zero/estado vazio**. Para gerar faturamento, um GERENTE deve finalizar pedidos reais.

**Documentação da nova atividade:** [análise, arquitetura, regras, API e testes em docs/DASHBOARD.md](docs/DASHBOARD.md). As rotas também estão listadas em [docs/API.md](docs/API.md).

## Executar com Docker

Requisitos: Docker e Docker Compose v2. Não é necessário instalar Java ou MongoDB nesta opção.

1. Copie `.env.example` para `.env`.
2. Preencha `BOOTSTRAP_ADMIN_SENHA` com uma senha sua, entre 12 e 64 caracteres. Use aspas simples no `.env` se a senha contiver `$` ou `#`.
3. Execute na pasta do projeto:

```bash
docker compose up --build
```

4. Acesse <http://localhost:8080>. Entre com o login definido em `.env` (padrão `admin`) e a senha escolhida.

No Windows PowerShell, a cópia pode ser feita com `Copy-Item .env.example .env`. No Linux/macOS, use `cp .env.example .env`.

O Compose sobe o MongoDB como replica set de um nó. Isso é necessário para gravar a operação e a auditoria na mesma transação. Espere a mensagem de inicialização do Spring antes de abrir a página; a primeira compilação baixa as dependências.

Para parar, use `docker compose down`. O volume mantém os cadastros. A senha de bootstrap só cria o primeiro ADMIN quando ainda não há usuários; mudar a variável depois não redefine senhas existentes.

A configuração do Compose é para demonstração local: as portas ficam vinculadas a `127.0.0.1`, e o MongoDB não possui autenticação nessa configuração. Uma implantação em servidor exige autenticação do banco, HTTPS e credenciais próprias.

## Executar com Java

Requisitos: JDK 17, acesso à internet na primeira compilação e MongoDB com replica set. O Maven Wrapper está incluído.

Pode-se usar apenas o banco do Compose:

```bash
docker compose up -d mongo
```

Antes disso, preencha o `.env`, pois o Compose valida as variáveis da configuração. Depois, configure a aplicação no terminal.

Linux/macOS:

```bash
export BOOTSTRAP_ADMIN_SENHA='sua-senha-com-12-caracteres-ou-mais'
export MONGODB_URI='mongodb://localhost:27017/pediupatrao?replicaSet=rs0&directConnection=true'
./mvnw spring-boot:run
```

PowerShell:

```powershell
$env:BOOTSTRAP_ADMIN_SENHA = 'sua-senha-com-12-caracteres-ou-mais'
$env:MONGODB_URI = 'mongodb://localhost:27017/pediupatrao?replicaSet=rs0&directConnection=true'
.\mvnw.cmd spring-boot:run
```

O Spring não lê o `.env` automaticamente fora do Compose. Informe as variáveis no terminal ou na configuração de execução da IDE. Se `./mvnw` perder a permissão de execução após a extração, use `chmod +x mvnw`.

Para gerar o JAR: `./mvnw clean package`. Para executar: `java -jar target/pediupatrao-1.0.0.jar`.

## Primeiro atendimento

1. Entre como ADMIN e cadastre uma conta GERENTE e uma ATENDENTE em **Usuários**. Os logins aceitam letras minúsculas, números, ponto, hífen e sublinhado.
2. Cadastre os produtos em **Cardápio**. Exemplo: Calabresa por R$ 50,00 e Margherita por R$ 55,00.
3. Saia e entre como ATENDENTE. Cadastre um cliente com telefone de 10 ou 11 dígitos e CEP de 8 dígitos, sem pontuação.
4. Registre o pedido e escolha entrega ou retirada.
5. Entre como GERENTE para encaminhar o pedido, aplicar desconto ou cancelar antes da saída.
6. Abra **Auditoria** para conferir os eventos. O detalhe do pedido possui um atalho para o histórico daquele registro.

## Perfis adotados

A tabela explicita as decisões para os pontos em que o enunciado não detalha cada operação.

| Operação | ADMIN | GERENTE | ATENDENTE |
| --- | :---: | :---: | :---: |
| Criar usuários e alterar perfis | Sim | Não | Não |
| Consultar pedidos | Sim | Sim | Sim |
| Visualizar Dashboard Administrativo | Sim | Sim | Não |
| Registrar pedidos | Não | Sim | Sim |
| Corrigir itens de pedido recebido | Não | Sim | Não |
| Avançar o status e registrar a saída | Não | Sim | Não |
| Aplicar desconto e cancelar | Não | Sim | Não |
| Consultar clientes | Não | Sim | Sim |
| Cadastrar, editar e excluir clientes | Não | Não | Sim |
| Consultar cardápio | Sim | Sim | Sim |
| Cadastrar, editar e inativar produtos | Sim | Sim | Não |
| Consultar auditoria | Sim | Sim | Não |

O GERENTE conduz os pedidos; o ATENDENTE registra e acompanha. Para preservar a manutenção do cardápio, ADMIN e GERENTE podem administrá-lo. A exclusão de clientes é lógica e fica com o ATENDENTE, responsável pelo cadastro. Não existe hierarquia que dê ao ADMIN as permissões do GERENTE.

Não há cadastro público. Somente o ADMIN cria usuários e atribui perfis. A própria conta não pode ter seu perfil ou situação alterados por essa operação. Mudanças de perfil e desativações são verificadas na próxima requisição, inclusive para sessões já abertas. Um pedido que já estava em execução no instante da mudança não é interrompido retroativamente.

## Regras dos pedidos

- Fluxo de entrega: `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO`.
- Fluxo de retirada: `RECEBIDO → EM_PREPARACAO → PRONTO → RETIRADO → FINALIZADO`.
- Só o GERENTE pode cancelar, e apenas em `RECEBIDO`, `EM_PREPARACAO` ou `PRONTO`. A justificativa precisa ter entre 5 e 300 caracteres após remover espaços das pontas.
- `CANCELADO` e `FINALIZADO` não possuem próxima etapa. Um cancelado não admite desconto, mudança de itens ou status.
- Itens só podem ser corrigidos em `RECEBIDO`, antes do encaminhamento à cozinha. A correção substitui a lista completa e usa os preços atuais do cardápio.
- O cadastro aceita de 1 a 30 produtos distintos, com quantidade de 1 a 99 por produto. Produtos inativos não entram em novos pedidos.
- O backend consulta os preços. A API recebe somente IDs e quantidades; `total`, `precoUnitario`, `registradoPor` e outros campos não previstos são rejeitados.
- Dinheiro usa `BigDecimal`. O desconto em reais é arredondado para duas casas com `HALF_UP`, e o total é o subtotal menos esse desconto.
- O desconto fica entre 0% e 20%, inclusive, antes da saída. Informar 0 remove o desconto. Um novo percentual substitui o anterior, sem acumular descontos sucessivos.
- Entrada, saída e finalização usam o relógio do servidor. Os horários são armazenados em UTC e apresentados em `America/Sao_Paulo`.
- Nome, telefone, endereço de entrega, descrição e preço dos produtos são copiados para o pedido. Alterações futuras no cadastro não reescrevem seu histórico.

Exemplo: duas pizzas de R$ 50,00 resultam em R$ 100,00. Um desconto de 10% gera R$ 10,00 de desconto e R$ 90,00 de total.

## Auditoria

`RegistroAuditoria`, `AuditoriaRepository` e `AuditoriaService` cuidam do histórico. Cada evento possui usuário, perfil, instante, operação, entidade, ID e uma lista com campo, valor anterior e valor novo. Cancelamentos incluem a justificativa. O sistema audita inclusões e alterações de pedidos, itens, status, descontos, cancelamentos, inclusões/alterações/exclusões lógicas de clientes e gestão de usuários.

As mudanças são calculadas a partir de campos explícitos. Nenhuma senha, hash de senha ou token CSRF entra nos eventos. Endereço e telefone permanecem visíveis aos perfis autorizados porque a conferência dessas alterações é exigida pela atividade. O acesso é restrito e a consulta é paginada; a aplicação não exporta o histórico publicamente.

A gravação do pedido/cliente e a inserção do evento compartilham a transação MongoDB. Se a inserção da auditoria falhar, a operação é revertida. As coleções são criadas antes das transações. Não há endpoint ou botão para editar/excluir eventos, e o repositório de auditoria expõe somente inserção e consulta. A proteção se refere à aplicação e às APIs comuns; acesso administrativo direto ao MongoDB deve ser controlado separadamente.

`@Version` protege as gravações contra alterações concorrentes. Conflitos não sobrescrevem silenciosamente uma versão já modificada. Uma repetição sem mudança de valores não cria um evento fictício de alteração.

## Onde estão os requisitos

| Requisito | Implementação principal |
| --- | --- |
| RF01 | `SecurityConfig`, `UsuarioDetailsService`, `AtualizarPermissoesFilter`, `UsuarioService` e `@PreAuthorize` nos serviços |
| RF02 | `Pedido`, `ItemPedido`, DTOs de entrada e cálculo em `PedidoService` |
| RF03 | `StatusPedido.proximos`, `PedidoService.mudarStatus`, entrada e saída com `Ator` e `Instant` |
| RF04 | `PedidoService.aplicarDesconto` e `cancelar`, com autorização exclusiva do GERENTE |
| RF05 | `RegistroAuditoria`, `AuditoriaRepository`, `AuditoriaService`, `RetratosAuditoria`, transações e tela de consulta |

## Estrutura

```text
src/main/java/br/com/pediupatrao/
  config/       Segurança, transações e primeiro administrador
  controller/   Rotas JSON, páginas e tratamento de erros
  dto/          Campos aceitos e validações de entrada
  exception/    Erros de negócio e registro ausente
  model/        Documentos MongoDB e tipos do domínio
  repository/   Persistência
  security/     Autenticação e identificação do usuário
  service/      Regras de negócio e auditoria
src/main/resources/
  templates/    Telas Thymeleaf
  static/       CSS e JavaScript dos formulários
src/test/java/br/com/pediupatrao/
  service/      Testes unitários dos pedidos
  FluxoCompletoIT.java  Integração HTTP, segurança e MongoDB
```

## Testes

Testes unitários, sem precisar de MongoDB:

```bash
./mvnw test
```

Os testes de integração precisam de um MongoDB real com replica set e um banco exclusivo cujo nome termine em `_test`. A preparação desses testes limpa os dados daquele banco. Não use o banco da aplicação.

Linux/macOS:

```bash
export MONGODB_TEST_URI='mongodb://localhost:27017/pediupatrao_test?replicaSet=rs0&directConnection=true'
./mvnw verify
```

PowerShell:

```powershell
$env:MONGODB_TEST_URI = 'mongodb://localhost:27017/pediupatrao_test?replicaSet=rs0&directConnection=true'
.\mvnw.cmd verify
```

Sem `MONGODB_TEST_URI`, os testes de integração ficam explicitamente ignorados. Isso não significa que passaram. Os relatórios do Maven ficam em `target/surefire-reports` e `target/failsafe-reports`.

Os cenários cobrem autenticação, roles, bloqueio de ADMIN e ATENDENTE, CSRF, cálculo de total, campos forjados, limites de desconto, transições, registro da saída, cancelamento, alterações de clientes, histórico, acesso à auditoria, sessões abertas e reversão quando a auditoria falha. Consulte `docs/VALIDACAO.md` para o resultado obtido na preparação desta entrega.

## Conferir o backend na demonstração

`docs/API.md` descreve as rotas e o uso de sessão/CSRF. `scripts/verificar_permissoes.py` tenta operações diretamente na API e mostra os códigos HTTP, sem depender dos botões das telas. Ele pede a senha no terminal e exige um ID de pedido válido, ainda em `RECEBIDO`, `EM_PREPARACAO` ou `PRONTO`.

Use o roteiro em `docs/ROTEIRO_VIDEO.md` para demonstrar os três perfis e os registros correspondentes. Execute o projeto e entenda cada regra antes de gravar. Não use dados pessoais reais na demonstração.


