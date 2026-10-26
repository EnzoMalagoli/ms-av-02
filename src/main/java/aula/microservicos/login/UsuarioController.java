package aula.microservicos.login;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import aula.microservicos.login.UsuarioService.ResultadoLogin;
import aula.microservicos.login.dto.CredenciaisRequest;
import aula.microservicos.login.dto.MensagemResponse;
import aula.microservicos.login.dto.TrocaSenhaRequest;
import aula.microservicos.login.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@OpenAPIDefinition(info = @Info(title = "Controle de Login", version = "1.0", description = "MS-AV-02 - Login, troca de senha, cadastro, desbloqueio e listagem de usuários"))
@RestController
public class UsuarioController {

    private final UsuarioService service;

    // Injeção de dependência do UsuarioService
    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // POST - http://localhost:8080/login
    @Operation(summary = "Efetua login", description = "Confere usuário e senha na tabela usuarios. Na 2ª falha seguida o usuário é bloqueado. Acesso público.", tags = { "login" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário e senha válidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "JSON inválido", content = @Content),
            @ApiResponse(responseCode = "401", description = "Usuário e senha inválidos ou usuário bloqueado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))) })
    @PostMapping("/login")
    public ResponseEntity<MensagemResponse> login(@Valid @RequestBody CredenciaisRequest credenciais) {

        ResultadoLogin resultado = service.login(credenciais.username(), credenciais.senha());

        if (resultado == ResultadoLogin.SUCESSO) {
            return new ResponseEntity<MensagemResponse>(new MensagemResponse("Login efetuado com sucesso"), HttpStatus.OK);
        } else if (resultado == ResultadoLogin.BLOQUEADO) {
            return new ResponseEntity<MensagemResponse>(new MensagemResponse("Usuário bloqueado"), HttpStatus.UNAUTHORIZED);
        } else {
            return new ResponseEntity<MensagemResponse>(new MensagemResponse("Usuário ou senha inválidos"), HttpStatus.UNAUTHORIZED);
        }
    }

    // PUT - http://localhost:8080/trocarsenha/joao
    @Operation(summary = "Troca a senha", description = "Confere a senha atual na base de dados e grava a nova senha. Acesso público.", tags = { "login" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha alterada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nova senha vazia ou com mais de 10 caracteres", content = @Content),
            @ApiResponse(responseCode = "401", description = "Senha atual incorreta ou usuário bloqueado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))) })
    @PutMapping("/trocarsenha/{userid}")
    public ResponseEntity<MensagemResponse> trocarSenha(
            @Parameter(description = "username do usuário", example = "joao") @PathVariable String userid,
            @Valid @RequestBody TrocaSenhaRequest troca) {

        service.trocarSenha(userid, troca.atual(), troca.nova());
        return new ResponseEntity<MensagemResponse>(new MensagemResponse("Senha alterada com sucesso"), HttpStatus.OK);
    }

    // POST - http://localhost:8080/
    @Operation(summary = "Cadastra usuário", description = "Cadastra um novo usuário (role ROLE_USER). Somente ROLE_MANUTENCAO.", tags = { "usuarios" }, security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem a role ROLE_MANUTENCAO", content = @Content),
            @ApiResponse(responseCode = "409", description = "Usuário já existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))) })
    @PostMapping("/")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CredenciaisRequest credenciais) {

        UsuarioResponse novo = service.cadastrar(credenciais.username(), credenciais.senha());
        return new ResponseEntity<UsuarioResponse>(novo, HttpStatus.CREATED);
    }

    // GET - http://localhost:8080/
    @Operation(summary = "Lista usuários", description = "Retorna todos os usuários cadastrados (sem a senha). Somente ROLE_MANUTENCAO.", tags = { "usuarios" }, security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem a role ROLE_MANUTENCAO", content = @Content) })
    @GetMapping("/")
    public ResponseEntity<List<UsuarioResponse>> listar() {

        return new ResponseEntity<List<UsuarioResponse>>(service.listar(), HttpStatus.OK);
    }

    // PUT - http://localhost:8080/unlock/carlos
    @Operation(summary = "Desbloqueia usuário", description = "Zera as falhas e desbloqueia o usuário. Somente ROLE_ADMIN.", tags = { "usuarios" }, security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário desbloqueado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem a role ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensagemResponse.class))) })
    @PutMapping("/unlock/{userid}")
    public ResponseEntity<MensagemResponse> desbloquear(
            @Parameter(description = "username do usuário", example = "carlos") @PathVariable String userid) {

        service.desbloquear(userid);
        return new ResponseEntity<MensagemResponse>(new MensagemResponse("Usuário " + userid + " desbloqueado"), HttpStatus.OK);
    }
}
