package aula.microservicos.login;

// Usada na troca de senha (senha atual incorreta ou usuário bloqueado)
public class SenhaInvalidaException extends RuntimeException {

    public SenhaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
