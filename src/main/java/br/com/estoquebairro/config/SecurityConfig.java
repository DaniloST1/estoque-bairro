package br.com.estoquebairro.config;

import br.com.estoquebairro.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository repo) {
    return login ->
        repo.byLogin(login.strip().toLowerCase(Locale.ROOT))
            .map(SessionUser::new)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, UserRepository repo) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/style.css", "/app.js", "/error")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?erro")
                    .permitAll())
        .logout(
            l ->
                l.logoutSuccessUrl("/login?saiu")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(e -> e.accessDeniedPage("/acesso-negado"))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                + " 'self' data:; base-uri 'self'; form-action 'self';"
                                + " frame-ancestors 'none'")))
        .addFilterBefore(
            new OncePerRequestFilter() {
              @Override
              protected void doFilterInternal(
                  HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                  throws IOException, ServletException {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof SessionUser p) {
                  try {
                    var current = repo.byId(p.id);
                    if (current.isEmpty()
                        || !current.get().ativo()
                        || current.get().versao() != p.version) {
                      SecurityContextHolder.clearContext();
                      if (request.getSession(false) != null) request.getSession(false).invalidate();
                      response.sendRedirect(request.getContextPath() + "/login?expirou");
                      return;
                    }
                  } catch (org.springframework.dao.DataAccessException e) {
                    response.setStatus(503);
                    response.setContentType("text/plain;charset=UTF-8");
                    response
                        .getWriter()
                        .write(
                            "Banco indisponível. Nenhuma operação foi confirmada nesta requisição."
                                + " Tente novamente.");
                    return;
                  }
                }
                chain.doFilter(request, response);
              }
            },
            AuthorizationFilter.class);
    return http.build();
  }
}
