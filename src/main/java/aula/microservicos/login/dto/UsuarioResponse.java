package aula.microservicos.login.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// Retorno do GET / (a senha nunca é devolvida)
@Schema(name = "Usuario", description = "Usuário cadastrado")
public record UsuarioResponse(

        @Schema(description = "Nome do usuário", example = "joao")
        String username,

        @Schema(description = "Roles do usuário", example = "[\"ROLE_USER\"]")
        List<String> roles,

        @Schema(description = "Indica se o usuário está bloqueado", example = "false")
        boolean bloqueado,

        @Schema(description = "Quantidade de falhas de login seguidas", example = "0")
        int tentativasFalhas) {
}
