package br.com.pediupatrao.config;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DadosIniciais implements ApplicationRunner {
    private final MongoTemplate mongo;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String login;
    private final String senha;
    public DadosIniciais(MongoTemplate mongo, UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${app.bootstrap.login}") String login, @Value("${app.bootstrap.senha}") String senha) {
        this.mongo=mongo; this.usuarios=usuarios; this.encoder=encoder; this.login=login; this.senha=senha;
    }
    @Override public void run(ApplicationArguments args) {
        // Coleções precisam existir antes das transações de negócio.
        for (Class<?> tipo : new Class<?>[]{Usuario.class, Cliente.class, Produto.class, Pedido.class, RegistroAuditoria.class})
            if (!mongo.collectionExists(tipo)) mongo.createCollection(tipo);
        if (usuarios.count() == 0) {
            if (!login.matches("[a-z0-9._-]{3,40}") || senha.length() < 12 || senha.length() > 64)
                throw new IllegalStateException("Configure BOOTSTRAP_ADMIN_LOGIN e BOOTSTRAP_ADMIN_SENHA (12 a 64 caracteres) para criar o primeiro administrador.");
            usuarios.insert(new Usuario(null,null,login,"Administrador",encoder.encode(senha),Perfil.ADMIN,true));
        }
    }
}
