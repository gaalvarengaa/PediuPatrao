package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF ativo para sessao do navegador; chamadas com Basic Auth nao usam cookie
                .csrf(csrf -> csrf.ignoringRequestMatchers(request -> {
                    String auth = request.getHeader("Authorization");
                    return auth != null && auth.startsWith("Basic ");
                }))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/acesso-negado", "/css/**", "/js/**", "/images/**").permitAll()

                        // Usuarios
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")

                        // Auditoria
                        .requestMatchers("/auditoria", "/auditoria/**", "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")

                        // Pedidos
                        .requestMatchers("/api/pedidos/*/desconto", "/api/pedidos/*/cancelar").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/pedidos/**", "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/**", "/api/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")

                        // Clientes
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/clientes/**", "/api/clientes/**").hasRole("ATENDENTE")

                        // Produtos
                        .requestMatchers(HttpMethod.GET, "/produtos/**", "/api/produtos/**").authenticated()
                        .requestMatchers("/produtos/**", "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Registra no log toda tentativa de acesso indevido (sem dados sensíveis) e
     * responde 403 em JSON para as APIs; nas páginas exibe a tela de acesso negado.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = auth != null ? auth.getName() : "anônimo";
            boolean csrf = ex instanceof CsrfException;
            log.warn("Acesso negado{}: usuario={} metodo={} uri={}",
                    csrf ? " (token CSRF ausente ou inválido)" : "", usuario, request.getMethod(), request.getRequestURI());

            if (request.getRequestURI().startsWith("/api/")) {
                String mensagem = csrf
                        ? "Token CSRF ausente ou inválido."
                        : "Acesso negado: seu perfil não tem permissão para esta operação.";
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
            } else {
                // Páginas: exibe a tela de acesso negado mantendo o status 403
                response.setStatus(HttpStatus.FORBIDDEN.value());
                request.getRequestDispatcher("/acesso-negado").forward(request, response);
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}
