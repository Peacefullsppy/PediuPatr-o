package br.com.pediupatrao.service;

import br.com.pediupatrao.dto.DashboardDTO.*;
import br.com.pediupatrao.exception.RegraNegocioException;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.PedidoRepository;
import java.math.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Leitura analítica dos pedidos existentes. Não altera pedidos, estoque ou auditoria. */
@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardService {
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_DIAS = 366;
    private final PedidoRepository repositorio;
    private final Clock clock;

    public DashboardService(PedidoRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public Dados consultar(Filtro filtro) {
        LocalDate hoje = LocalDate.now(clock.withZone(FUSO));
        LocalDate fim = filtro.fim() == null ? hoje : filtro.fim();
        LocalDate inicio = filtro.inicio() == null ? fim.minusDays(29) : filtro.inicio();
        if (inicio.isAfter(fim))
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final.");
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAX_DIAS)
            throw new RegraNegocioException("O intervalo máximo é de 366 dias, incluindo as duas datas.");
        Agrupamento agrupamento = filtro.agrupamento() != null ? filtro.agrupamento()
                : ChronoUnit.DAYS.between(inicio, fim) <= 45 ? Agrupamento.DIA
                : ChronoUnit.DAYS.between(inicio, fim) <= 180 ? Agrupamento.SEMANA : Agrupamento.MES;

        // Início inclusivo e fim exclusivo (meia-noite do dia seguinte) no fuso do negócio.
        Instant de = inicio.atStartOfDay(FUSO).toInstant();
        Instant ate = fim.plusDays(1).atStartOfDay(FUSO).toInstant();
        List<Pedido> encontrados = repositorio.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(de, ate);
        List<Pedido> selecionados = encontrados.stream()
                .filter(p -> filtro.status() == null || p.status() == filtro.status())
                .filter(p -> filtro.tipo() == null || p.tipoAtendimento() == filtro.tipo())
                .toList();

        long finalizados = selecionados.stream().filter(p -> p.status() == StatusPedido.FINALIZADO).count();
        long cancelados = selecionados.stream().filter(p -> p.status() == StatusPedido.CANCELADO).count();
        long emAndamento = selecionados.size() - finalizados - cancelados;
        BigDecimal faturamento = selecionados.stream().filter(p -> p.status() == StatusPedido.FINALIZADO)
                .map(Pedido::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ticket = finalizados == 0 ? dinheiro(0) : faturamento.divide(BigDecimal.valueOf(finalizados), 2, RoundingMode.HALF_UP);
        BigDecimal taxa = selecionados.isEmpty() ? dinheiro(0) : BigDecimal.valueOf(cancelados * 100L)
                .divide(BigDecimal.valueOf(selecionados.size()), 2, RoundingMode.HALF_UP);
        Resumo resumo = new Resumo(faturamento, finalizados, selecionados.size(), ticket, cancelados, taxa, emAndamento);

        Map<LocalDate, Acumulado> porPeriodo = new TreeMap<>();
        for (LocalDate atual = chave(inicio, agrupamento); !atual.isAfter(fim); atual = avancar(atual, agrupamento))
            porPeriodo.put(atual, new Acumulado());
        Map<StatusPedido, Long> porStatus = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) porStatus.put(status, 0L);
        Map<String, ProdutoAcumulado> porProduto = new HashMap<>();

        for (Pedido pedido : selecionados) {
            porStatus.merge(pedido.status(), 1L, Long::sum);
            if (pedido.status() != StatusPedido.FINALIZADO) continue;
            LocalDate dia = pedido.entradaEm().atZone(FUSO).toLocalDate();
            Acumulado grupo = porPeriodo.get(chave(dia, agrupamento));
            if (grupo != null) {
                grupo.valor = grupo.valor.add(pedido.total());
                grupo.pedidos++;
            }
            // Proporciona o desconto do pedido a cada item; o resíduo de centavos
            // fica no último item para preservar exatamente o valor faturado.
            BigDecimal restante = pedido.total();
            List<ItemPedido> itens = pedido.itens();
            for (int i = 0; i < itens.size(); i++) {
                ItemPedido item = itens.get(i);
                BigDecimal parte = i == itens.size() - 1 ? restante : pedido.subtotal().signum() == 0
                    ? dinheiro(0) : pedido.total().multiply(item.getSubtotal())
                        .divide(pedido.subtotal(), 2, RoundingMode.HALF_UP);
                restante = restante.subtract(parte);
                ProdutoAcumulado produto = porProduto.computeIfAbsent(item.produtoId(),
                        id -> new ProdutoAcumulado(id, item.descricao()));
                produto.quantidade += item.quantidade();
                produto.faturamento = produto.faturamento.add(parte);
            }
        }

        List<Venda> vendas = porPeriodo.entrySet().stream()
                .map(e -> new Venda(e.getKey(), e.getValue().valor, e.getValue().pedidos)).toList();
        List<Situacao> situacao = Arrays.stream(StatusPedido.values())
                .map(s -> new Situacao(s, s.getDescricao(), porStatus.get(s))).toList();
        List<ProdutoMaisVendido> produtos = porProduto.values().stream()
                .sorted(Comparator.comparingLong((ProdutoAcumulado p) -> p.quantidade).reversed()
                    .thenComparing((ProdutoAcumulado p) -> p.faturamento, Comparator.reverseOrder())
                    .thenComparing(p -> p.nome))
                .limit(6).map(p -> new ProdutoMaisVendido(p.id, p.nome, p.quantidade, p.faturamento)).toList();
        List<PedidoRecente> recentes = selecionados.stream()
                .sorted(Comparator.comparing(Pedido::entradaEm).reversed().thenComparing(Pedido::id))
                .limit(8).map(p -> new PedidoRecente(p.id(), p.getNumero(), p.clienteNome(),
                    p.entradaEm(), p.tipoAtendimento(), p.status(), p.total())).toList();
        Periodo periodo = new Periodo(inicio, fim, FUSO.getId(), agrupamento,
                filtro.status(), filtro.tipo(), clock.instant());
        return new Dados(periodo, resumo, vendas, situacao, produtos, recentes);
    }

    private static LocalDate chave(LocalDate dia, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }
    private static LocalDate avancar(LocalDate data, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> data.plusDays(1);
            case SEMANA -> data.plusWeeks(1);
            case MES -> data.plusMonths(1);
        };
    }
    private static BigDecimal dinheiro(long valor) { return BigDecimal.valueOf(valor).setScale(2); }
    private static final class Acumulado {
        BigDecimal valor = dinheiro(0);
        long pedidos;
    }
    private static final class ProdutoAcumulado {
        final String id, nome;
        long quantidade;
        BigDecimal faturamento = dinheiro(0);
        ProdutoAcumulado(String id, String nome) { this.id = id; this.nome = nome; }
    }
}
