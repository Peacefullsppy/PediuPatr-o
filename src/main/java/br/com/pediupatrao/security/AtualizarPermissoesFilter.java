package br.com.pediupatrao.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
public class AtualizarPermissoesFilter extends OncePerRequestFilter {
    private final UsuarioDetailsService detalhes;
    public AtualizarPermissoesFilter(UsuarioDetailsService detalhes) { this.detalhes = detalhes; }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken && auth.isAuthenticated()) {
            try {
                var atual = detalhes.loadUserByUsername(auth.getName());
                if (!atual.isEnabled()) throw new UsernameNotFoundException("Usuário inativo.");
                // Mudanças de perfil passam a valer na próxima requisição, inclusive em sessões abertas.
                var renovada = UsernamePasswordAuthenticationToken.authenticated(atual, null, atual.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(renovada);
            } catch (UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                var session = req.getSession(false);
                if (session != null) session.invalidate();
            }
        }
        chain.doFilter(req, res);
    }
}
