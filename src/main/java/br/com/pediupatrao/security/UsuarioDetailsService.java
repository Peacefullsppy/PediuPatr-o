package br.com.pediupatrao.security;
import br.com.pediupatrao.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.*;
@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public UsuarioDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override public UserDetails loadUserByUsername(String login) {
        var usuario = usuarios.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return User.withUsername(usuario.login()).password(usuario.senhaHash())
            .roles(usuario.perfil().name()).disabled(!usuario.ativo()).build();
    }
}
