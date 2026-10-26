package aula.microservicos.login;

import static org.springframework.security.config.Customizer.withDefaults;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// Habilita o botão "Authorize" (Basic Auth) no Swagger
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain defauSecurityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                // necessário para o console do H2 abrir dentro de frames
                .headers(header -> header.frameOptions(hconfig -> hconfig.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(withDefaults())
                .formLogin(config -> config.disable())
                .authorizeHttpRequests(auth -> auth
                        // endpoints públicos do enunciado
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/trocarsenha/*").permitAll()

                        // POST / e GET / -> somente ROLE_MANUTENCAO
                        // (mesmo caminho, por isso a regra filtra pelo método HTTP)
                        .requestMatchers(HttpMethod.POST, "/").hasRole("MANUTENCAO")
                        .requestMatchers(HttpMethod.GET, "/").hasRole("MANUTENCAO")

                        // PUT /unlock/{userid} -> somente ROLE_ADMIN
                        .requestMatchers(HttpMethod.PUT, "/unlock/*").hasRole("ADMIN")

                        // documentação, métricas (Prometheus) e console do H2
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // página de erro padrão do Spring (senão 400/404 viram 401)
                        .requestMatchers("/error").permitAll()

                        // qualquer outra coisa é negada
                        .anyRequest().denyAll());

        return http.build();
    }

    // Usuários e roles lidos das tabelas users / authorities
    @Bean
    public JdbcUserDetailsManager userDetailsServiceDataSource(DataSource datasource) {

        return new JdbcUserDetailsManager(datasource);
    }

    // Permite senhas com prefixo {noop}, {bcrypt}, etc.
    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return encoder;
    }
}
