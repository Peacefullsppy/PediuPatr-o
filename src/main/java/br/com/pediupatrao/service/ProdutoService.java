package br.com.pediupatrao.service;
import br.com.pediupatrao.dto.Formularios.ProdutoForm;
import br.com.pediupatrao.model.Produto;
import br.com.pediupatrao.repository.ProdutoRepository;
import br.com.pediupatrao.exception.NaoEncontradoException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
public class ProdutoService {
    private final ProdutoRepository produtos;
    public ProdutoService(ProdutoRepository produtos) { this.produtos=produtos; }
    public List<Produto> listar() { return produtos.findAllByOrderByNomeAsc(); }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(String id, ProdutoForm dados) {
        Long versao=null;
        if (id != null) versao=produtos.findById(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado.")).versao();
        return produtos.save(new Produto(id,versao,dados.nome().trim(),dados.preco().setScale(2),dados.ativo()));
    }
}
