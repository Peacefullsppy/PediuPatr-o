package br.com.pediupatrao;
import br.com.pediupatrao.dto.Formularios.*;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.*;
import br.com.pediupatrao.service.PedidoService;
import com.fasterxml.jackson.databind.*;
import com.mongodb.ConnectionString;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"app.bootstrap.login=admin","app.bootstrap.senha=SenhaInicial123!"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="MONGODB_TEST_URI",matches=".+")
class FluxoCompletoIT {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry registry) {
        String uri=System.getenv("MONGODB_TEST_URI");
        if (uri != null) {
            String banco=new ConnectionString(uri).getDatabase();
            if (banco==null || !banco.endsWith("_test")) throw new IllegalArgumentException("Use um banco exclusivo terminado em _test.");
            registry.add("spring.data.mongodb.uri",() -> uri);
        }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired ProdutoRepository produtos;
    @Autowired PedidoRepository pedidos;
    @Autowired PedidoService pedidoService;
    @Autowired PasswordEncoder encoder;
    @Autowired MongoTemplate mongo;
    @MockitoSpyBean AuditoriaRepository auditoria;
    String clienteId,produtoId,adminId;
    @BeforeEach void preparar() {
        pedidos.deleteAll(); clientes.deleteAll(); produtos.deleteAll(); usuarios.deleteAll();
        mongo.remove(new org.springframework.data.mongodb.core.query.Query(),RegistroAuditoria.class);
        for (Perfil perfil:Perfil.values()) {
            var u=usuarios.insert(new Usuario(null,null,perfil.name().toLowerCase(),perfil.name(),encoder.encode("SenhaTeste123!"),perfil,true));
            if (perfil==Perfil.ADMIN) adminId=u.id();
        }
        clienteId=clientes.insert(new Cliente(null,null,"Ana Teste","11987654321",new Endereco("08710000","Rua das Flores","12","","Centro","Mogi das Cruzes"),true)).id();
        produtoId=produtos.insert(new Produto(null,null,"Calabresa",new BigDecimal("50.00"),true)).id();
    }
    String corpoPedido(String tipo) throws Exception {
        return json.writeValueAsString(new PedidoForm(clienteId,TipoAtendimento.valueOf(tipo),List.of(new ItemForm(produtoId,2))));
    }
    JsonNode criarPedido(String tipo) throws Exception {
        var r=mvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).with(csrf())
            .contentType("application/json").content(corpoPedido(tipo))).andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }
    ResultActions postar(String login,String caminho,String corpo) throws Exception {
        return mvc.perform(post(caminho).with(user(login).roles(login.toUpperCase())).with(csrf()).contentType("application/json").content(corpo));
    }
    void statusPedido(String id,String status) throws Exception {
        postar("gerente","/api/pedidos/"+id+"/status","{\"status\":\""+status+"\"}").andExpect(status().isOk());
    }
    @Test void dashboardExigeGerenteOuAdminEValidaFiltros() throws Exception {
        mvc.perform(get("/dashboard").with(user("atendente").roles("ATENDENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard").with(user("atendente").roles("ATENDENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/dashboard").with(user("gerente").roles("GERENTE"))).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard?inicio=2026-11-05&fim=2026-11-04")
            .with(user("admin").roles("ADMIN"))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/dashboard?status=INEXISTENTE")
            .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
    }
    @Test void dashboardMostraDadosReaisAposFinalizacaoSemContarCanceladosComoVenda() throws Exception {
        String finalizado=criarPedido("ENTREGA").get("id").asText();
        statusPedido(finalizado,"EM_PREPARACAO"); statusPedido(finalizado,"PRONTO");
        statusPedido(finalizado,"SAIU_PARA_ENTREGA"); statusPedido(finalizado,"FINALIZADO");
        String cancelado=criarPedido("RETIRADA").get("id").asText();
        postar("gerente","/api/pedidos/"+cancelado+"/cancelamento","{\"justificativa\":\"Cliente desistiu\"}").andExpect(status().isOk());
        String hoje=java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
        mvc.perform(get("/api/dashboard").param("inicio",hoje).param("fim",hoje)
            .with(user("gerente").roles("GERENTE"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.resumo.totalPedidos").value(2))
            .andExpect(jsonPath("$.resumo.faturamento").value(100.0))
            .andExpect(jsonPath("$.resumo.pedidosCancelados").value(1))
            .andExpect(jsonPath("$.produtos[0].quantidade").value(2));
        mvc.perform(get("/api/dashboard/resumo").param("inicio",hoje).param("fim",hoje)
            .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.pedidosFinalizados").value(1));
    }
    @Test void autenticaOsTresPerfisComSuasRolesReais() throws Exception {
        for (String login:List.of("admin","gerente","atendente")) {
            var r=mvc.perform(post("/login").with(csrf()).param("username",login).param("password","SenhaTeste123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pedidos")).andReturn();
            var sessao=(MockHttpSession)r.getRequest().getSession(false);
            mvc.perform(get("/pedidos").session(sessao)).andExpect(status().isOk());
            mvc.perform(get("/api/usuarios").session(sessao)).andExpect(status().is(login.equals("admin") ? 200 : 403));
        }
    }
    @Test void credenciaisInvalidasNaoAutenticam() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username","admin").param("password","errada"))
            .andExpect(redirectedUrl("/login?error"));
    }
    @Test void exigeSessaoParaLerPedidos() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }
    @Test void exigeCsrfMesmoParaUsuarioAutorizado() throws Exception {
        mvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE"))
            .contentType("application/json").content(corpoPedido("RETIRADA"))).andExpect(status().isForbidden());
        assertThat(pedidos.count()).isZero();
    }
    @Test void calculaTotalComCatalogoERejeitaCamposForjados() throws Exception {
        var p=criarPedido("ENTREGA");
        assertThat(p.get("total").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(p.get("registradoPor").get("usuario").asText()).isEqualTo("atendente");
        assertThat(p.get("entradaEm").isNull()).isFalse();
        var adulterado=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(corpoPedido("RETIRADA"));
        adulterado.put("total",1);
        postar("atendente","/api/pedidos",adulterado.toString()).andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isEqualTo(1);
    }
    @Test void adminEAtendenteNaoDescontamNemCancelamNemMudamStatus() throws Exception {
        String id=criarPedido("RETIRADA").get("id").asText();
        for (String login:List.of("admin","atendente")) {
            postar(login,"/api/pedidos/"+id+"/desconto","{\"percentual\":10}").andExpect(status().isForbidden());
            postar(login,"/api/pedidos/"+id+"/cancelamento","{\"justificativa\":\"Cliente desistiu\"}").andExpect(status().isForbidden());
            postar(login,"/api/pedidos/"+id+"/status","{\"status\":\"EM_PREPARACAO\"}").andExpect(status().isForbidden());
        }
        assertThat(pedidos.findById(id).orElseThrow().status()).isEqualTo(StatusPedido.RECEBIDO);
    }
    @Test @WithMockUser(username="admin",roles="ADMIN")
    void protegeServicoMesmoSemPassarPeloController() {
        assertThatThrownBy(() -> pedidoService.aplicarDesconto("qualquer",new DescontoForm(BigDecimal.TEN)))
            .isInstanceOf(AccessDeniedException.class);
    }
    @Test void gerenteDescontaAteVintePorCentoEAuditaValores() throws Exception {
        String id=criarPedido("RETIRADA").get("id").asText();
        postar("gerente","/api/pedidos/"+id+"/desconto","{\"percentual\":20}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(80));
        postar("gerente","/api/pedidos/"+id+"/desconto","{\"percentual\":20.01}").andExpect(status().isBadRequest());
        var eventos=auditoria.consultar("PEDIDO",id,org.springframework.data.domain.PageRequest.of(0,20));
        assertThat(eventos.getContent()).anySatisfy(e -> {
            assertThat(e.operacao()).isEqualTo("DESCONTO_PEDIDO"); assertThat(e.usuario()).isEqualTo("gerente");
            assertThat(e.perfil()).isEqualTo(Perfil.GERENTE); assertThat(e.ocorridoEm()).isNotNull();
            assertThat(e.alteracoes()).anySatisfy(a -> { assertThat(a.campo()).isEqualTo("total"); assertThat(a.anterior()).isEqualTo("100.00"); assertThat(a.novo()).isEqualTo("80.00"); });
        });
    }
    @Test void percorreEntregaERetiradaComRegistroDeSaida() throws Exception {
        for (String tipo:List.of("ENTREGA","RETIRADA")) {
            String id=criarPedido(tipo).get("id").asText();
            postar("gerente","/api/pedidos/"+id+"/status","{\"status\":\"FINALIZADO\"}").andExpect(status().isUnprocessableEntity());
            statusPedido(id,"EM_PREPARACAO"); statusPedido(id,"PRONTO");
            String saida=tipo.equals("ENTREGA") ? "SAIU_PARA_ENTREGA" : "RETIRADO";
            statusPedido(id,saida);
            var p=pedidos.findById(id).orElseThrow();
            assertThat(p.saidaEm()).isNotNull(); assertThat(p.saidaPor().usuario()).isEqualTo("gerente");
            postar("gerente","/api/pedidos/"+id+"/desconto","{\"percentual\":5}").andExpect(status().isUnprocessableEntity());
            postar("gerente","/api/pedidos/"+id+"/cancelamento","{\"justificativa\":\"Cliente desistiu\"}").andExpect(status().isUnprocessableEntity());
            statusPedido(id,"FINALIZADO"); assertThat(pedidos.findById(id).orElseThrow().finalizadoEm()).isNotNull();
        }
    }
    @Test void cancelamentoExigeMotivoEPedidoFicaImutavel() throws Exception {
        String id=criarPedido("RETIRADA").get("id").asText();
        postar("gerente","/api/pedidos/"+id+"/cancelamento","{\"justificativa\":\"   \"}").andExpect(status().isBadRequest());
        postar("gerente","/api/pedidos/"+id+"/cancelamento","{\"justificativa\":\"Cliente desistiu\"}").andExpect(status().isOk());
        postar("gerente","/api/pedidos/"+id+"/status","{\"status\":\"EM_PREPARACAO\"}").andExpect(status().isUnprocessableEntity());
        postar("gerente","/api/pedidos/"+id+"/desconto","{\"percentual\":10}").andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/pedidos/"+id+"/itens").with(user("gerente").roles("GERENTE")).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(new ItensForm(List.of(new ItemForm(produtoId,3)))))).andExpect(status().isUnprocessableEntity());
        assertThat(pedidos.findById(id).orElseThrow().total()).isEqualByComparingTo("100.00");
        assertThat(auditoria.consultar("PEDIDO",id,org.springframework.data.domain.PageRequest.of(0,20)).getContent())
            .anySatisfy(e -> assertThat(e.justificativa()).isEqualTo("Cliente desistiu"));
    }
    @Test void alteraItensAuditaECalculaDescontoNovamente() throws Exception {
        String id=criarPedido("RETIRADA").get("id").asText();
        postar("gerente","/api/pedidos/"+id+"/desconto","{\"percentual\":10}");
        mvc.perform(put("/api/pedidos/"+id+"/itens").with(user("gerente").roles("GERENTE")).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(new ItensForm(List.of(new ItemForm(produtoId,3)))))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(135));
        assertThat(auditoria.consultar("PEDIDO",id,org.springframework.data.domain.PageRequest.of(0,20)).getContent())
            .anySatisfy(e -> { assertThat(e.operacao()).isEqualTo("ALTERACAO_ITENS"); assertThat(e.alteracoes()).anySatisfy(a -> assertThat(a.campo()).isEqualTo("itens[0].quantidade")); });
    }
    ClienteForm clienteAlterado() {
        return new ClienteForm("Ana Teste","11999998888",new Endereco("08710000","Av. Brasil","12","","Centro","Mogi das Cruzes"));
    }
    @Test void auditaTelefoneEEnderecoSemAlterarHistoricoDoPedido() throws Exception {
        String id=criarPedido("ENTREGA").get("id").asText();
        mvc.perform(put("/api/clientes/"+clienteId).with(user("atendente").roles("ATENDENTE")).with(csrf())
            .contentType("application/json").content(json.writeValueAsString(clienteAlterado()))).andExpect(status().isOk());
        var eventos=auditoria.consultar("CLIENTE",clienteId,org.springframework.data.domain.PageRequest.of(0,20));
        assertThat(eventos.getContent()).singleElement().satisfies(e -> {
            assertThat(e.alteracoes()).anySatisfy(a -> { assertThat(a.campo()).isEqualTo("logradouro"); assertThat(a.anterior()).isEqualTo("Rua das Flores"); assertThat(a.novo()).isEqualTo("Av. Brasil"); });
            assertThat(e.alteracoes()).anySatisfy(a -> assertThat(a.campo()).isEqualTo("telefone"));
        });
        assertThat(pedidos.findById(id).orElseThrow().enderecoEntrega().logradouro()).isEqualTo("Rua das Flores");
    }
    @Test void falhaNaAuditoriaDesfazAlteracaoDoCliente() throws Exception {
        doThrow(new DataAccessResourceFailureException("Falha simulada")).when(auditoria).inserir(any());
        mvc.perform(put("/api/clientes/"+clienteId).with(user("atendente").roles("ATENDENTE")).with(csrf())
            .contentType("application/json").content(json.writeValueAsString(clienteAlterado()))).andExpect(status().isServiceUnavailable());
        assertThat(clientes.findById(clienteId).orElseThrow().endereco().logradouro()).isEqualTo("Rua das Flores");
    }
    @Test void falhaNaAuditoriaDesfazInclusaoDoPedido() throws Exception {
        doThrow(new DataAccessResourceFailureException("Falha simulada")).when(auditoria).inserir(any());
        postar("atendente","/api/pedidos",corpoPedido("RETIRADA")).andExpect(status().isServiceUnavailable());
        assertThat(pedidos.count()).isZero();
    }
    @Test void somenteAdminGerenciaUsuariosESemSenhaNaRespostaOuAuditoria() throws Exception {
        String corpo="{\"login\":\"cozinha\",\"nome\":\"Cozinha\",\"senha\":\"SenhaCriada123!\",\"perfil\":\"GERENTE\"}";
        for (String login:List.of("gerente","atendente")) postar(login,"/api/usuarios",corpo).andExpect(status().isForbidden());
        var r=postar("admin","/api/usuarios",corpo).andExpect(status().isCreated()).andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContain("senha","Hash","SenhaCriada");
        assertThat(usuarios.findByLogin("cozinha").orElseThrow().senhaHash()).startsWith("$2");
        var logs=mvc.perform(get("/api/auditoria").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn();
        assertThat(logs.getResponse().getContentAsString()).doesNotContain("senha","Hash","SenhaCriada");
    }
    @Test void naoPermiteAutoalteracaoDoAdminOuElevacaoPeloAtendente() throws Exception {
        mvc.perform(put("/api/usuarios/"+adminId+"/perfil").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"perfil\":\"GERENTE\",\"ativo\":true}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/usuarios/"+adminId+"/perfil").with(user("atendente").roles("ATENDENTE")).with(csrf())
            .contentType("application/json").content("{\"perfil\":\"ADMIN\",\"ativo\":true}")).andExpect(status().isForbidden());
    }
    @Test void mudancaDePerfilValeParaSessaoJaAberta() throws Exception {
        var r=mvc.perform(post("/login").with(csrf()).param("username","gerente").param("password","SenhaTeste123!")).andReturn();
        var sessao=(MockHttpSession)r.getRequest().getSession(false);
        var gerente=usuarios.findByLogin("gerente").orElseThrow();
        mvc.perform(put("/api/usuarios/"+gerente.id()+"/perfil").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"perfil\":\"ATENDENTE\",\"ativo\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").session(sessao)).andExpect(status().isForbidden());
    }
    @Test void auditoriaTemConsultaRestritaENaoPodeSerEditadaOuExcluida() throws Exception {
        for (String login:List.of("admin","gerente","atendente")) {
            mvc.perform(get("/api/auditoria").with(user(login).roles(login.toUpperCase())))
                .andExpect(status().is(login.equals("atendente") ? 403 : 200));
            for (var metodo:List.of(org.springframework.http.HttpMethod.POST,org.springframework.http.HttpMethod.PUT,org.springframework.http.HttpMethod.DELETE)) {
                mvc.perform(request(metodo,"/api/auditoria/qualquer").with(user(login).roles(login.toUpperCase())).with(csrf()))
                    .andExpect(status().isForbidden());
            }
        }
    }
    @Test void renderizaTelasDeCadastroPedidosEAuditoria() throws Exception {
        String id=criarPedido("RETIRADA").get("id").asText();
        for(String rota:List.of("/clientes","/clientes/novo","/clientes/"+clienteId+"/editar","/pedidos/novo"))
            mvc.perform(get(rota).with(user("atendente").roles("ATENDENTE"))).andExpect(status().isOk());
        for(String rota:List.of("/pedidos/"+id,"/produtos","/auditoria"))
            mvc.perform(get(rota).with(user("gerente").roles("GERENTE"))).andExpect(status().isOk());
        mvc.perform(get("/usuarios").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
    @Test void cadastroEExclusaoLogicaDoClienteGeramHistorico() throws Exception {
        var r=postar("atendente","/api/clientes",json.writeValueAsString(clienteAlterado()))
            .andExpect(status().isCreated()).andReturn();
        String id=json.readTree(r.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(delete("/api/clientes/"+id).with(user("atendente").roles("ATENDENTE")).with(csrf()))
            .andExpect(status().isNoContent());
        assertThat(clientes.findById(id).orElseThrow().ativo()).isFalse();
        assertThat(auditoria.consultar("CLIENTE",id,org.springframework.data.domain.PageRequest.of(0,20)).getTotalElements()).isEqualTo(2);
    }
    @Test void contaDesativadaPerdeAcessoNaSessaoAberta() throws Exception {
        var r=mvc.perform(post("/login").with(csrf()).param("username","atendente").param("password","SenhaTeste123!")).andReturn();
        var sessao=(MockHttpSession)r.getRequest().getSession(false);
        var atendente=usuarios.findByLogin("atendente").orElseThrow();
        mvc.perform(put("/api/usuarios/"+atendente.id()+"/perfil").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"perfil\":\"ATENDENTE\",\"ativo\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/pedidos").session(sessao)).andExpect(status().isUnauthorized());
    }
    @Test void precoPosteriorDoCardapioNaoReescrevePedidoExistente() throws Exception {
        String id=criarPedido("RETIRADA").get("id").asText();
        mvc.perform(put("/api/produtos/"+produtoId).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"nome\":\"Calabresa\",\"preco\":60,\"ativo\":true}")).andExpect(status().isOk());
        assertThat(pedidos.findById(id).orElseThrow().itens().get(0).precoUnitario()).isEqualByComparingTo("50.00");
        assertThat(criarPedido("RETIRADA").get("total").decimalValue()).isEqualByComparingTo("120.00");
    }
    @Test void quantidadeFracionariaNaoPodeSerTruncadaPeloJson() throws Exception {
        var corpo=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(corpoPedido("RETIRADA"));
        ((com.fasterxml.jackson.databind.node.ObjectNode)corpo.get("itens").get(0)).put("quantidade",1.5);
        postar("atendente","/api/pedidos",corpo.toString()).andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero();
    }
}
