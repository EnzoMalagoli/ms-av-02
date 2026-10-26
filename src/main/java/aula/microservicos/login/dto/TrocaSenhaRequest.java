package aula.microservicos.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// JSON {atual: "", nova: ""} usado no PUT /trocarsenha/{userid}
@Schema(name = "TrocaSenha", description = "Senha atual e nova senha")
public record TrocaSenhaRequest(

        @Schema(description = "Senha atual", example = "joao123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "senha atual é obrigatória")
        String atual,

        @Schema(description = "Nova senha (máximo 10 caracteres)", example = "nova123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "nova senha é obrigatória")
        @Size(max = 10, message = "nova senha deve ter no máximo 10 caracteres")
        String nova) {
}
