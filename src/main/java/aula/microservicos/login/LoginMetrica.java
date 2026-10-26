package aula.microservicos.login;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

// Indicadores pedidos no enunciado.
// No Prometheus eles aparecem como:
//   login_validos_total     (counter)
//   login_invalidos_total   (counter)
//   usuarios_bloqueados     (gauge - lido direto da tabela login_attempts)
@Component
public class LoginMetrica {

    private final Counter loginsValidos;
    private final Counter loginsInvalidos;

    public LoginMetrica(MeterRegistry registry, LoginAttemptRepository attemptRepo) {

        loginsValidos = Counter.builder("login.validos")
                .description("Quantidade de logins válidos")
                .register(registry);

        loginsInvalidos = Counter.builder("login.invalidos")
                .description("Quantidade de logins inválidos")
                .register(registry);

        // o gauge consulta o banco a cada leitura, então sempre mostra o total atual de bloqueados
        Gauge.builder("usuarios.bloqueados", attemptRepo, repo -> repo.countByLockedTrue())
                .description("Total de usuários bloqueados")
                .register(registry);
    }

    public void loginValido() {
        loginsValidos.increment();
    }

    public void loginInvalido() {
        loginsInvalidos.increment();
    }
}
