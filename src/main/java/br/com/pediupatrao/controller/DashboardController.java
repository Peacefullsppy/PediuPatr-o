package br.com.pediupatrao.controller;

import br.com.pediupatrao.dto.DashboardDTO.*;
import br.com.pediupatrao.model.StatusPedido;
import br.com.pediupatrao.model.TipoAtendimento;
import br.com.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** Rotas de leitura; todas exigem sessão com perfil ADMIN ou GERENTE. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    public record Parametros(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                             StatusPedido status, TipoAtendimento tipo, Agrupamento agrupamento) {
        Filtro filtro() { return new Filtro(inicio, fim, status, tipo, agrupamento); }
    }
    @GetMapping
    public Dados painel(@ModelAttribute Parametros parametros) { return service.consultar(parametros.filtro()); }
    @GetMapping("/resumo")
    public Resumo resumo(@ModelAttribute Parametros parametros) { return painel(parametros).resumo(); }
    @GetMapping("/vendas")
    public java.util.List<Venda> vendas(@ModelAttribute Parametros parametros) { return painel(parametros).vendas(); }
    @GetMapping("/situacao")
    public java.util.List<Situacao> situacao(@ModelAttribute Parametros parametros) { return painel(parametros).situacao(); }
    @GetMapping("/produtos")
    public java.util.List<ProdutoMaisVendido> produtos(@ModelAttribute Parametros parametros) { return painel(parametros).produtos(); }
    @GetMapping("/pedidos")
    public java.util.List<PedidoRecente> pedidos(@ModelAttribute Parametros parametros) { return painel(parametros).pedidos(); }
}
