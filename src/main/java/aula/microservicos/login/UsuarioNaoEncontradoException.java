package aula.microservicos.login;

public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(String username) {
        super("Usuário " + username + " não encontrado");
    }
}
