package br.com.pediupatrao.repository;
import br.com.pediupatrao.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface UsuarioRepository extends MongoRepository<Usuario, String> { java.util.Optional<Usuario> findByLogin(String login); boolean existsByLogin(String login); }
