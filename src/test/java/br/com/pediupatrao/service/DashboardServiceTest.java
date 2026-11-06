package br.com.pediupatrao.service;

import br.com.pediupatrao.dto.DashboardDTO.*;
import br.com.pediupatrao.exception.RegraNegocioException;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private final PedidoRepository repo = mock(PedidoRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T14:00:00Z"), ZoneOffset.UTC);
    private final DashboardService service = new DashboardService(repo, clock);

    private Pedido pedido(String id, String instante, StatusPedido status, TipoAtendimento tipo,
                          String subtotal, String total, ItemPedido... itens) {
        return new Pedido(id, 0L, "c1", "Ana", "11999990000", null, tipo,
            List.of(itens), new BigDecimal(subtotal), BigDecimal.ZERO,
            new BigDecimal(subtotal).subtract(new BigDecimal(total)), new BigDecimal(total),
            status, new Ator("atendente",Perfil.ATENDENTE), Instant.parse(instante),
            null, null, null, null, null);
    }
    private ItemPedido item(String id, String nome, int qtd, String valor) {
        return new ItemPedido(id,nome,qtd,new BigDecimal(valor));
    }
    private Filtro filtro() { return new Filtro(LocalDate.parse("2026-11-03"),LocalDate.parse("2026-11-04"),null,null,Agrupamento.DIA); }

    @Test void contabilizaApenasFinalizadosEAplicaDescontoNoRanking() {
        var primeiro = pedido("p1","2026-11-03T03:00:00Z",StatusPedido.FINALIZADO,TipoAtendimento.ENTREGA,
            "50.00","45.00",item("pizza","Pizza",2,"20.00"),item("refri","Refrigerante",1,"10.00"));
        var segundo = pedido("p2","2026-11-04T14:00:00Z",StatusPedido.FINALIZADO,TipoAtendimento.RETIRADA,
            "40.00","40.00",item("pizza","Pizza",2,"20.00"));
        var cancelado = pedido("p3","2026-11-03T19:00:00Z",StatusPedido.CANCELADO,TipoAtendimento.ENTREGA,
            "500.00","500.00",item("premium","Premium",1,"500.00"));
        var aberto = pedido("p4","2026-11-04T10:00:00Z",StatusPedido.RECEBIDO,TipoAtendimento.ENTREGA,
            "99.00","99.00",item("outro","Outro",1,"99.00"));
        when(repo.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(any(),any()))
            .thenReturn(List.of(primeiro,segundo,cancelado,aberto));
        var resposta=service.consultar(filtro());
        assertThat(resposta.resumo().totalPedidos()).isEqualTo(4);
        assertThat(resposta.resumo().pedidosFinalizados()).isEqualTo(2);
        assertThat(resposta.resumo().pedidosCancelados()).isEqualTo(1);
        assertThat(resposta.resumo().pedidosEmAndamento()).isEqualTo(1);
        assertThat(resposta.resumo().faturamento()).isEqualByComparingTo("85.00");
        assertThat(resposta.resumo().ticketMedio()).isEqualByComparingTo("42.50");
        assertThat(resposta.resumo().taxaCancelamento()).isEqualByComparingTo("25.00");
        assertThat(resposta.vendas()).extracting(Venda::faturamento)
            .containsExactly(new BigDecimal("45.00"),new BigDecimal("40.00"));
        assertThat(resposta.produtos()).extracting(ProdutoMaisVendido::nome).containsExactly("Pizza","Refrigerante");
        assertThat(resposta.produtos().get(0).quantidade()).isEqualTo(4);
        assertThat(resposta.produtos().get(0).faturamento()).isEqualByComparingTo("76.00");
        assertThat(resposta.produtos().get(1).faturamento()).isEqualByComparingTo("9.00");
        verify(repo).findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(
            Instant.parse("2026-11-03T03:00:00Z"),Instant.parse("2026-11-05T03:00:00Z"));
    }

    @Test void filtraStatusETipoDeAtendimento() {
        when(repo.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(any(),any()))
            .thenReturn(List.of(
                pedido("p1","2026-11-04T14:00:00Z",StatusPedido.FINALIZADO,TipoAtendimento.ENTREGA,"10.00","10.00",item("p","Pizza",1,"10.00")),
                pedido("p2","2026-11-04T14:00:00Z",StatusPedido.FINALIZADO,TipoAtendimento.RETIRADA,"20.00","20.00",item("p","Pizza",1,"20.00")),
                pedido("p3","2026-11-04T14:00:00Z",StatusPedido.CANCELADO,TipoAtendimento.ENTREGA,"30.00","30.00",item("p","Pizza",1,"30.00"))));
        var resposta=service.consultar(new Filtro(filtro().inicio(),filtro().fim(),StatusPedido.FINALIZADO,TipoAtendimento.ENTREGA,Agrupamento.DIA));
        assertThat(resposta.resumo().totalPedidos()).isEqualTo(1);
        assertThat(resposta.resumo().faturamento()).isEqualByComparingTo("10.00");
        assertThat(resposta.pedidos()).extracting(PedidoRecente::id).containsExactly("p1");
    }

    @Test void naoInventaReceitaQuandoNaoHaDadosEPreencheGraficoComZeros() {
        when(repo.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(any(),any())).thenReturn(List.of());
        var resposta=service.consultar(filtro());
        assertThat(resposta.resumo().faturamento()).isEqualByComparingTo("0.00");
        assertThat(resposta.resumo().ticketMedio()).isEqualByComparingTo("0.00");
        assertThat(resposta.produtos()).isEmpty();
        assertThat(resposta.vendas()).hasSize(2);
        assertThat(resposta.vendas()).allSatisfy(v -> assertThat(v.faturamento()).isEqualByComparingTo("0.00"));
        assertThat(resposta.situacao()).hasSize(StatusPedido.values().length);
    }

    @Test void validaDatasEIntervaloMaximo() {
        assertThatThrownBy(() -> service.consultar(new Filtro(LocalDate.parse("2026-11-04"),LocalDate.parse("2026-11-03"),null,null,null)))
            .isInstanceOf(RegraNegocioException.class).hasMessageContaining("data inicial");
        assertThatThrownBy(() -> service.consultar(new Filtro(LocalDate.parse("2025-01-01"),LocalDate.parse("2026-11-04"),null,null,null)))
            .isInstanceOf(RegraNegocioException.class).hasMessageContaining("366 dias");
        verifyNoInteractions(repo);
    }

    @Test void semanaComecaNaSegundaEPeriodoPadraoUsaFusoDaPizzaria() {
        when(repo.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(any(),any())).thenReturn(List.of());
        var semanal=service.consultar(new Filtro(LocalDate.parse("2026-11-01"),LocalDate.parse("2026-11-04"),null,null,Agrupamento.SEMANA));
        assertThat(semanal.vendas()).extracting(Venda::periodo)
            .containsExactly(LocalDate.parse("2026-10-26"),LocalDate.parse("2026-11-02"));
        var padrao=service.consultar(new Filtro(null,null,null,null,null));
        assertThat(padrao.periodo().fim()).isEqualTo(LocalDate.parse("2026-11-04"));
        assertThat(padrao.periodo().inicio()).isEqualTo(LocalDate.parse("2026-10-06"));
        assertThat(padrao.periodo().fuso()).isEqualTo("America/Sao_Paulo");
    }
}
