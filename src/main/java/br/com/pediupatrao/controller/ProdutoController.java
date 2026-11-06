package br.com.pediupatrao.controller;
import br.com.pediupatrao.dto.Formularios.ProdutoForm;
import br.com.pediupatrao.model.Produto;
import br.com.pediupatrao.service.ProdutoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/produtos")
public class ProdutoController {
    private final ProdutoService produtos;
    public ProdutoController(ProdutoService produtos) { this.produtos=produtos; }
    @GetMapping public List<Produto> listar() { return produtos.listar(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Produto criar(@Valid @RequestBody ProdutoForm dados) { return produtos.salvar(null,dados); }
    @PutMapping("/{id}") public Produto atualizar(@PathVariable String id,@Valid @RequestBody ProdutoForm dados) { return produtos.salvar(id,dados); }
}
