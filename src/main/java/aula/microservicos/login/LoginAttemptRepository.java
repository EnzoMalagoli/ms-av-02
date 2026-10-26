package aula.microservicos.login;

import org.springframework.data.repository.CrudRepository;

public interface LoginAttemptRepository extends CrudRepository<LoginAttempt, String> {

    // consulta derivada: SELECT COUNT(*) FROM login_attempts WHERE locked = TRUE
    long countByLockedTrue();

}
