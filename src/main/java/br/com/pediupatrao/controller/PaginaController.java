package br.com.pediupatrao.controller;
import br.com.pediupatrao.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class PaginaController {
    private final PedidoService pedidos;
    private final ClienteService clientes;
    private final ProdutoService produtos;
    private final UsuarioService usuarios;
    private final AuditoriaService auditoria;
    public PaginaController(PedidoService pedidos,ClienteService clientes,ProdutoService produtos,UsuarioService usuarios,AuditoriaService auditoria) {
        this.pedidos=pedidos; this.clientes=clientes; this.produtos=produtos; this.usuarios=usuarios; this.auditoria=auditoria;
    }
    @GetMapping("/dashboard") public String dashboard() { return "dashboard"; }
    @GetMapping("/") public String inicio() { return "redirect:/pedidos"; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/pedidos") public String pedidos(@RequestParam(defaultValue="0") int pagina,Model model) {
        model.addAttribute("pagina",pedidos.listar(pagina)); return "pedidos/lista";
    }
    @GetMapping("/pedidos/novo") public String novoPedido(Model model) {
        model.addAttribute("clientes",clientes.listar()); model.addAttribute("produtos",produtos.listar()); return "pedidos/novo";
    }
    @GetMapping("/pedidos/{id}") public String pedido(@PathVariable String id,Model model) {
        model.addAttribute("pedido",pedidos.buscar(id)); model.addAttribute("produtos",produtos.listar()); return "pedidos/detalhe";
    }
    @GetMapping("/clientes") public String clientes(Model model) { model.addAttribute("clientes",clientes.listar()); return "clientes/lista"; }
    @GetMapping("/clientes/novo") @PreAuthorize("hasRole('ATENDENTE')")
    public String novoCliente() { return "clientes/formulario"; }
    @GetMapping("/clientes/{id}/editar") @PreAuthorize("hasRole('ATENDENTE')")
    public String editarCliente(@PathVariable String id,Model model) { model.addAttribute("cliente",clientes.buscar(id)); return "clientes/formulario"; }
    @GetMapping("/produtos") public String produtos(Model model) { model.addAttribute("produtos",produtos.listar()); return "produtos"; }
    @GetMapping("/usuarios") public String usuarios(Model model) { model.addAttribute("usuarios",usuarios.listar()); return "usuarios"; }
    @GetMapping("/auditoria") public String auditoria(@RequestParam(required=false) String entidade,
            @RequestParam(required=false) String entidadeId,@RequestParam(defaultValue="0") int pagina,Model model) {
        model.addAttribute("pagina",auditoria.consultar(entidade,entidadeId,pagina));
        model.addAttribute("entidade",entidade); model.addAttribute("entidadeId",entidadeId); return "auditoria";
    }
}
