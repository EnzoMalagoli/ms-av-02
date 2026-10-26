package aula.microservicos.login;

public class UsuarioJaExisteException extends RuntimeException {

    public UsuarioJaExisteException(String username) {
        super("Usuário " + username + " já existe");
    }
}
