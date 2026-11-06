package br.com.pediupatrao.controller;
import br.com.pediupatrao.dto.Formularios.ClienteForm;
import br.com.pediupatrao.model.Cliente;
import br.com.pediupatrao.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService clientes;
    public ClienteController(ClienteService clientes) { this.clientes=clientes; }
    @GetMapping public List<Cliente> listar() { return clientes.listar(); }
    @GetMapping("/{id}") public Cliente buscar(@PathVariable String id) { return clientes.buscar(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Cliente criar(@Valid @RequestBody ClienteForm dados) { return clientes.criar(dados); }
    @PutMapping("/{id}") public Cliente atualizar(@PathVariable String id,@Valid @RequestBody ClienteForm dados) { return clientes.atualizar(id,dados); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluir(@PathVariable String id) { clientes.excluir(id); }
}
