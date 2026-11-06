package br.com.pediupatrao.security;
import br.com.pediupatrao.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class UsuarioAtual {
    public Ator obter() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Autenticação necessária.");
        for (Perfil perfil : Perfil.values()) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + perfil.name())))
                return new Ator(auth.getName(), perfil);
        }
        throw new AccessDeniedException("Perfil de acesso inválido.");
    }
}
