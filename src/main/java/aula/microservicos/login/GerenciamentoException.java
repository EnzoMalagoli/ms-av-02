package aula.microservicos.login;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import aula.microservicos.login.dto.MensagemResponse;

@RestControllerAdvice
public class GerenciamentoException {

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<MensagemResponse> naoEncontrado(UsuarioNaoEncontradoException e) {
        return new ResponseEntity<MensagemResponse>(new MensagemResponse(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsuarioJaExisteException.class)
    public ResponseEntity<MensagemResponse> jaExiste(UsuarioJaExisteException e) {
        return new ResponseEntity<MensagemResponse>(new MensagemResponse(e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SenhaInvalidaException.class)
    public ResponseEntity<MensagemResponse> senhaInvalida(SenhaInvalidaException e) {
        return new ResponseEntity<MensagemResponse>(new MensagemResponse(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    // JSON sem o corpo esperado ou mal formatado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MensagemResponse> jsonInvalido(HttpMessageNotReadableException e) {
        return new ResponseEntity<MensagemResponse>(new MensagemResponse("JSON inválido ou ausente"),
                HttpStatus.BAD_REQUEST);
    }

    // Especifico para validação de campos
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<String, String>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }
}
