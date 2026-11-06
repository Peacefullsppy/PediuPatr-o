package br.com.pediupatrao.service;
import br.com.pediupatrao.dto.Formularios.*;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.*;
import br.com.pediupatrao.security.UsuarioAtual;
import br.com.pediupatrao.exception.RegraNegocioException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PedidoServiceTest {
    PedidoRepository pedidos=mock(PedidoRepository.class);
    ProdutoRepository produtos=mock(ProdutoRepository.class);
    AuditoriaService auditoria=mock(AuditoriaService.class);
    UsuarioAtual atual=mock(UsuarioAtual.class);
    Clock clock=Clock.fixed(Instant.parse("2026-10-15T15:00:00Z"),ZoneOffset.UTC);
    PedidoService service=new PedidoService(pedidos,mock(ClienteRepository.class),produtos,auditoria,atual,clock);
    @BeforeEach void configurar() {
        when(atual.obter()).thenReturn(new Ator("gerente",Perfil.GERENTE));
        when(pedidos.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    Pedido pedido(StatusPedido status,TipoAtendimento tipo,String valor) {
        var preco=new BigDecimal(valor);
        var p=new Pedido("pedido1",0L,"cliente1","Cliente teste","11988887777",null,tipo,
            List.of(new ItemPedido("pizza","Pizza",1,preco)),preco,new BigDecimal("0.00"),new BigDecimal("0.00"),preco,
            status,new Ator("atendente",Perfil.ATENDENTE),clock.instant(),null,null,null,null,null);
        when(pedidos.findById("pedido1")).thenReturn(Optional.of(p)); return p;
    }
    @Test void descontoArredondaEmCentavosESubstituiPercentualAnterior() {
        pedido(StatusPedido.RECEBIDO,TipoAtendimento.RETIRADA,"49.95");
        var comDesconto=service.aplicarDesconto("pedido1",new DescontoForm(new BigDecimal("10")));
        assertThat(comDesconto.valorDesconto()).isEqualByComparingTo("5.00");
        assertThat(comDesconto.total()).isEqualByComparingTo("44.95");
        when(pedidos.findById("pedido1")).thenReturn(Optional.of(comDesconto));
        var novo=service.aplicarDesconto("pedido1",new DescontoForm(new BigDecimal("20")));
        assertThat(novo.total()).isEqualByComparingTo("39.96");
    }
    @ParameterizedTest @ValueSource(strings={"-1","20.01","100","1.001"})
    void rejeitaPercentualInvalido(String percentual) {
        pedido(StatusPedido.RECEBIDO,TipoAtendimento.RETIRADA,"50.00");
        assertThatThrownBy(() -> service.aplicarDesconto("pedido1",new DescontoForm(new BigDecimal(percentual))))
            .isInstanceOf(RegraNegocioException.class);
        verify(pedidos,never()).save(any());
    }
    @ParameterizedTest @EnumSource(StatusPedido.class)
    void validaTodosOsDestinosDeCadaEstado(StatusPedido origem) {
        for (TipoAtendimento tipo:TipoAtendimento.values()) {
            for (StatusPedido destino:StatusPedido.values()) {
                pedido(origem,tipo,"50.00");
                boolean permitido=switch(origem) {
                    case RECEBIDO -> destino==StatusPedido.EM_PREPARACAO;
                    case EM_PREPARACAO -> destino==StatusPedido.PRONTO;
                    case PRONTO -> destino==(tipo==TipoAtendimento.ENTREGA ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO);
                    case SAIU_PARA_ENTREGA, RETIRADO -> destino==StatusPedido.FINALIZADO;
                    default -> false;
                };
                if (permitido) assertThat(service.mudarStatus("pedido1",new StatusForm(destino)).status()).isEqualTo(destino);
                else assertThatThrownBy(() -> service.mudarStatus("pedido1",new StatusForm(destino))).isInstanceOf(RegraNegocioException.class);
            }
        }
    }
    @Test void registraResponsavelEHorarioNaSaida() {
        pedido(StatusPedido.PRONTO,TipoAtendimento.ENTREGA,"50.00");
        var saida=service.mudarStatus("pedido1",new StatusForm(StatusPedido.SAIU_PARA_ENTREGA));
        assertThat(saida.saidaPor().usuario()).isEqualTo("gerente"); assertThat(saida.saidaEm()).isEqualTo(clock.instant());
    }
    @ParameterizedTest @EnumSource(value=StatusPedido.class,names={"CANCELADO","FINALIZADO","RETIRADO","SAIU_PARA_ENTREGA"})
    void bloqueiaDescontoECancelamentoAposEncerramentoOuSaida(StatusPedido status) {
        pedido(status,TipoAtendimento.RETIRADA,"50.00");
        assertThatThrownBy(() -> service.aplicarDesconto("pedido1",new DescontoForm(BigDecimal.TEN))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.cancelar("pedido1",new CancelamentoForm("Cliente desistiu"))).isInstanceOf(RegraNegocioException.class);
    }
    @Test void cancelamentoExigeJustificativaNaoVazia() {
        pedido(StatusPedido.RECEBIDO,TipoAtendimento.RETIRADA,"50.00");
        assertThatThrownBy(() -> service.cancelar("pedido1",new CancelamentoForm("      "))).isInstanceOf(RegraNegocioException.class);
    }
    @Test void cancelamentoMantemItensValoresEEntrada() {
        var antes=pedido(StatusPedido.PRONTO,TipoAtendimento.RETIRADA,"50.00");
        var depois=service.cancelar("pedido1",new CancelamentoForm("Cliente desistiu"));
        assertThat(depois.itens()).isEqualTo(antes.itens()); assertThat(depois.total()).isEqualTo(antes.total());
        assertThat(depois.entradaEm()).isEqualTo(antes.entradaEm()); assertThat(depois.canceladoEm()).isNotNull();
    }
    @Test void naoAlteraItensDepoisDeEncaminharACozinha() {
        pedido(StatusPedido.EM_PREPARACAO,TipoAtendimento.RETIRADA,"50.00");
        assertThatThrownBy(() -> service.alterarItens("pedido1",new ItensForm(List.of(new ItemForm("pizza",2)))))
            .isInstanceOf(RegraNegocioException.class);
    }
    @Test void usaPrecoDoCatalogoAoSubstituirItens() {
        pedido(StatusPedido.RECEBIDO,TipoAtendimento.RETIRADA,"50.00");
        when(produtos.findById("pizza")).thenReturn(Optional.of(new Produto("pizza",0L,"Calabresa",new BigDecimal("42.90"),true)));
        var novo=service.alterarItens("pedido1",new ItensForm(List.of(new ItemForm("pizza",3))));
        assertThat(novo.total()).isEqualByComparingTo("128.70");
    }
    @Test void rejeitaProdutoRepetido() {
        pedido(StatusPedido.RECEBIDO,TipoAtendimento.RETIRADA,"50.00");
        when(produtos.findById("pizza")).thenReturn(Optional.of(new Produto("pizza",0L,"Calabresa",new BigDecimal("42.90"),true)));
        assertThatThrownBy(() -> service.alterarItens("pedido1",new ItensForm(List.of(new ItemForm("pizza",1),new ItemForm("pizza",1)))))
            .isInstanceOf(RegraNegocioException.class);
    }
}
