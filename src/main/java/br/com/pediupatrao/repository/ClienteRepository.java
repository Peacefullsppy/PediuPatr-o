package br.com.pediupatrao.repository;
import br.com.pediupatrao.model.Cliente;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ClienteRepository extends MongoRepository<Cliente, String> { java.util.List<Cliente> findByAtivoTrueOrderByNomeAsc(); }
