package aula.microservicos.login;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import aula.microservicos.login.dto.UsuarioResponse;

@Service
public class UsuarioService {

    // Quantidade de falhas a partir da qual o usuário é bloqueado.
    // Enunciado: "Caso o usuário falhe mais de uma vez no login ele deve ser bloqueado"
    // -> na 2ª falha seguida o usuário é bloqueado
    public static final int MAX_FALHAS = 2;

    public enum ResultadoLogin {
        SUCESSO, INVALIDO, BLOQUEADO
    }

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepo;
    private final LoginAttemptRepository attemptRepo;
    private final JdbcUserDetailsManager userDetailsManager;
    private final LoginMetrica metrica;

    // Injeção de dependência via construtor
    public UsuarioService(UsuarioRepository usuarioRepo, LoginAttemptRepository attemptRepo,
            JdbcUserDetailsManager userDetailsManager, LoginMetrica metrica) {
        this.usuarioRepo = usuarioRepo;
        this.attemptRepo = attemptRepo;
        this.userDetailsManager = userDetailsManager;
        this.metrica = metrica;
    }

    // ------------------------------------------------------------------
    // POST /login
    // ------------------------------------------------------------------
    @Transactional
    public ResultadoLogin login(String username, String senha) {

        Optional<Usuario> resultado = usuarioRepo.findById(username);

        // usuário não existe
        if (resultado.isEmpty()) {
            metrica.loginInvalido();
            log.info("Login inválido: usuário {} inexistente", username);
            return ResultadoLogin.INVALIDO;
        }

        LoginAttempt tentativa = obterTentativa(username);

        // usuário bloqueado não consegue logar, mesmo com a senha certa
        if (Boolean.TRUE.equals(tentativa.getLocked())) {
            metrica.loginInvalido();
            log.info("Login negado: usuário {} está bloqueado", username);
            return ResultadoLogin.BLOQUEADO;
        }

        LocalDateTime agora = LocalDateTime.now();

        // senha correta -> zera as falhas
        if (resultado.get().getSenha().equals(senha)) {
            tentativa.setFailedAttempts(0);
            tentativa.setUpdatedAt(agora);
            attemptRepo.save(tentativa);
            metrica.loginValido();
            log.info("Login válido: {}", username);
            return ResultadoLogin.SUCESSO;
        }

        // senha errada -> registra a falha
        int falhas = tentativa.getFailedAttempts() + 1;
        tentativa.setFailedAttempts(falhas);
        tentativa.setLastFailedAt(agora);
        tentativa.setUpdatedAt(agora);
        metrica.loginInvalido();

        // falhou mais de uma vez -> bloqueia
        if (falhas >= MAX_FALHAS) {
            tentativa.setLocked(true);
            tentativa.setLockedAt(agora);
            attemptRepo.save(tentativa);
            habilitarUsuarioSeguranca(username, false);
            log.warn("Usuário {} bloqueado após {} falhas de login", username, falhas);
            return ResultadoLogin.BLOQUEADO;
        }

        attemptRepo.save(tentativa);
        log.info("Login inválido: senha incorreta para {} ({} falha)", username, falhas);
        return ResultadoLogin.INVALIDO;
    }

    // ------------------------------------------------------------------
    // PUT /trocarsenha/{userid}
    // ------------------------------------------------------------------
    @Transactional
    public void trocarSenha(String username, String atual, String nova) {

        Usuario usuario = usuarioRepo.findById(username)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(username));

        if (Boolean.TRUE.equals(obterTentativa(username).getLocked())) {
            throw new SenhaInvalidaException("Usuário " + username + " está bloqueado");
        }

        // confere a senha atual com a base de dados
        if (!usuario.getSenha().equals(atual)) {
            throw new SenhaInvalidaException("Senha atual incorreta");
        }

        usuario.setSenha(nova);
        usuarioRepo.save(usuario);

        // mantém a senha do Spring Security (tabela users) igual
        if (userDetailsManager.userExists(username)) {
            UserDetails atualSeguranca = userDetailsManager.loadUserByUsername(username);
            userDetailsManager.updateUser(User.withUserDetails(atualSeguranca)
                    .password("{noop}" + nova)
                    .build());
        }

        log.info("Senha alterada: {}", username);
    }

    // ------------------------------------------------------------------
    // POST /
    // ------------------------------------------------------------------
    @Transactional
    public UsuarioResponse cadastrar(String username, String senha) {

        if (usuarioRepo.existsById(username) || userDetailsManager.userExists(username)) {
            throw new UsuarioJaExisteException(username);
        }

        // tabela usuarios (usada no /login)
        usuarioRepo.save(new Usuario(username, senha));

        // tabela login_attempts (controle de bloqueio)
        attemptRepo.save(new LoginAttempt(username));

        // tabelas users / authorities via JdbcUserDetailsManager
        userDetailsManager.createUser(User.withUsername(username)
                .password("{noop}" + senha)
                .roles("USER")
                .build());

        log.info("Usuário cadastrado: {}", username);
        return montarResposta(username);
    }

    // ------------------------------------------------------------------
    // GET /
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {

        List<UsuarioResponse> lista = new ArrayList<UsuarioResponse>();
        for (Usuario u : usuarioRepo.findAll()) {
            lista.add(montarResposta(u.getUsername()));
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // PUT /unlock/{userid}
    // ------------------------------------------------------------------
    @Transactional
    public void desbloquear(String username) {

        if (!usuarioRepo.existsById(username)) {
            throw new UsuarioNaoEncontradoException(username);
        }

        LoginAttempt tentativa = obterTentativa(username);
        tentativa.setFailedAttempts(0);
        tentativa.setLocked(false);
        tentativa.setLockedAt(null);
        tentativa.setUpdatedAt(LocalDateTime.now());
        attemptRepo.save(tentativa);

        habilitarUsuarioSeguranca(username, true);

        log.info("Usuário desbloqueado: {}", username);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    // retorna a linha do login_attempts do usuário (cria se ainda não existir)
    private LoginAttempt obterTentativa(String username) {
        return attemptRepo.findById(username).orElseGet(() -> new LoginAttempt(username));
    }

    // bloqueia / desbloqueia também no Spring Security (coluna enabled da tabela users),
    // assim um usuário bloqueado não consegue acessar endpoints protegidos
    private void habilitarUsuarioSeguranca(String username, boolean habilitado) {
        if (userDetailsManager.userExists(username)) {
            UserDetails atual = userDetailsManager.loadUserByUsername(username);
            userDetailsManager.updateUser(User.withUserDetails(atual)
                    .disabled(!habilitado)
                    .build());
        }
    }

    private UsuarioResponse montarResposta(String username) {

        List<String> roles = new ArrayList<String>();
        if (userDetailsManager.userExists(username)) {
            for (GrantedAuthority a : userDetailsManager.loadUserByUsername(username).getAuthorities()) {
                roles.add(a.getAuthority());
            }
        }

        LoginAttempt tentativa = obterTentativa(username);
        return new UsuarioResponse(username, roles,
                Boolean.TRUE.equals(tentativa.getLocked()),
                tentativa.getFailedAttempts() == null ? 0 : tentativa.getFailedAttempts());
    }
}
