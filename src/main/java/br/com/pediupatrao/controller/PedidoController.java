package br.com.pediupatrao.controller;
import br.com.pediupatrao.dto.Formularios.*;
import br.com.pediupatrao.model.Pedido;
import br.com.pediupatrao.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidos;
    public PedidoController(PedidoService pedidos) { this.pedidos=pedidos; }
    @GetMapping public Page<Pedido> listar(@RequestParam(defaultValue="0") int pagina) { return pedidos.listar(pagina); }
    @GetMapping("/{id}") public Pedido buscar(@PathVariable String id) { return pedidos.buscar(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Pedido criar(@Valid @RequestBody PedidoForm dados) { return pedidos.criar(dados); }
    @PutMapping("/{id}/itens") public Pedido itens(@PathVariable String id, @Valid @RequestBody ItensForm dados) { return pedidos.alterarItens(id,dados); }
    @PostMapping("/{id}/status") public Pedido status(@PathVariable String id, @Valid @RequestBody StatusForm dados) { return pedidos.mudarStatus(id,dados); }
    @PostMapping("/{id}/desconto") public Pedido desconto(@PathVariable String id, @Valid @RequestBody DescontoForm dados) { return pedidos.aplicarDesconto(id,dados); }
    @PostMapping("/{id}/cancelamento") public Pedido cancelar(@PathVariable String id, @Valid @RequestBody CancelamentoForm dados) { return pedidos.cancelar(id,dados); }
}
