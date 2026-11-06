package br.com.pediupatrao.config;
import br.com.pediupatrao.security.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain seguranca(HttpSecurity http, UsuarioDetailsService detalhes) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/css/**", "/js/**", "/error", "/api/csrf").permitAll()
            .requestMatchers("/dashboard", "/api/dashboard", "/api/dashboard/**").hasAnyRole("ADMIN", "GERENTE")
            .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/auditoria/**", "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
            .requestMatchers("/auditoria/**", "/api/auditoria/**").denyAll()
            .requestMatchers(HttpMethod.POST, "/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento", "/api/pedidos/*/status").hasRole("GERENTE")
            .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/itens").hasRole("GERENTE")
            .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole("GERENTE", "ATENDENTE")
            .requestMatchers(HttpMethod.GET, "/pedidos/novo").hasAnyRole("GERENTE", "ATENDENTE")
            .requestMatchers(HttpMethod.GET, "/pedidos/**", "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
            .requestMatchers(HttpMethod.GET, "/clientes/**", "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
            .requestMatchers("/api/clientes/**").hasRole("ATENDENTE")
            .requestMatchers(HttpMethod.GET, "/produtos/**", "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
            .requestMatchers("/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")
            .requestMatchers("/").authenticated()
            .anyRequest().denyAll())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/pedidos", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor((req,res,e) -> {
                    res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"mensagem\":\"Entre na sua conta para continuar.\"}");
                }, new AntPathRequestMatcher("/api/**"))
                .accessDeniedHandler((req,res,e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"mensagem\":\"Operação não permitida ou token CSRF inválido.\"}");
                    } else res.sendError(403);
                }))
            .addFilterBefore(new AtualizarPermissoesFilter(detalhes), AuthorizationFilter.class);
        // CSRF continua ativo tanto para formulários quanto para a API com sessão.
        return http.build();
    }
}
