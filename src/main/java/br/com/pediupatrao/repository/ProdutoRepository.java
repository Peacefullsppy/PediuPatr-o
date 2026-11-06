package br.com.pediupatrao.repository;
import br.com.pediupatrao.model.Produto;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ProdutoRepository extends MongoRepository<Produto, String> { java.util.List<Produto> findAllByOrderByNomeAsc(); }
