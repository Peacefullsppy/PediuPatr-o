package br.com.pediupatrao.dto;

import br.com.pediupatrao.model.StatusPedido;
import br.com.pediupatrao.model.TipoAtendimento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contrato público de leitura do dashboard. Nenhum dado pessoal sensível é retornado. */
public final class DashboardDTO {
    private DashboardDTO() {}
    public enum Agrupamento { DIA, SEMANA, MES }

    public record Filtro(LocalDate inicio, LocalDate fim, StatusPedido status,
                         TipoAtendimento tipo, Agrupamento agrupamento) {}
    public record Periodo(LocalDate inicio, LocalDate fim, String fuso,
                          Agrupamento agrupamento, StatusPedido status,
                          TipoAtendimento tipo, Instant atualizadoEm) {}
    public record Resumo(BigDecimal faturamento, long pedidosFinalizados,
                         long totalPedidos, BigDecimal ticketMedio,
                         long pedidosCancelados, BigDecimal taxaCancelamento,
                         long pedidosEmAndamento) {}
    public record Venda(LocalDate periodo, BigDecimal faturamento, long pedidos) {}
    public record Situacao(StatusPedido status, String descricao, long quantidade) {}
    public record ProdutoMaisVendido(String produtoId, String nome, long quantidade, BigDecimal faturamento) {}
    public record PedidoRecente(String id, String numero, String cliente,
                                Instant entradaEm, TipoAtendimento tipo,
                                StatusPedido status, BigDecimal total) {}
    public record Dados(Periodo periodo, Resumo resumo, List<Venda> vendas,
                        List<Situacao> situacao, List<ProdutoMaisVendido> produtos,
                        List<PedidoRecente> pedidos) {}
}
