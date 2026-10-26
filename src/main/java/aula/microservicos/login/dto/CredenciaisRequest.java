package aula.microservicos.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// JSON {username: "", senha: ""} usado no POST /login e no POST /
@Schema(name = "Credenciais", description = "Usuário e senha")
public record CredenciaisRequest(

        @Schema(description = "Nome do usuário", example = "joao", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "username é obrigatório")
        @Size(max = 100, message = "username deve ter no máximo 100 caracteres")
        String username,

        @Schema(description = "Senha do usuário (máximo 10 caracteres)", example = "joao123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "senha é obrigatória")
        @Size(max = 10, message = "senha deve ter no máximo 10 caracteres")
        String senha) {
}
