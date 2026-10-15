package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public Usuario salvarUsuario(Usuario usuario) {
        String username = validarUsername(usuario.getUsername(), null);
        validarPerfil(usuario.getRole());

        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }

        Usuario novo = new Usuario();
        novo.setUsername(username);
        novo.setRole(usuario.getRole());
        novo.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(novo);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id, String usernameLogado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (usuario.getUsername().equals(usernameLogado)) {
            throw new IllegalArgumentException("Você não pode excluir o próprio usuário.");
        }

        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado, String usernameLogado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validarPerfil(usuarioAtualizado.getRole());

        boolean proprioUsuario = usuarioExistente.getUsername().equals(usernameLogado);
        if (proprioUsuario && !usuarioAtualizado.getRole().equals(usuarioExistente.getRole())) {
            throw new IllegalArgumentException("Você não pode alterar o próprio perfil.");
        }

        String novoUsername = validarUsername(usuarioAtualizado.getUsername(), id);
        // A sessão identifica o usuário pelo username: renomear a própria conta permitiria
        // escapar das verificações acima (alterar o próprio perfil ou excluir a própria conta)
        if (proprioUsuario && !novoUsername.equals(usuarioExistente.getUsername())) {
            throw new IllegalArgumentException("Você não pode alterar o próprio username.");
        }

        usuarioExistente.setUsername(novoUsername);
        usuarioExistente.setRole(usuarioAtualizado.getRole());

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        return usuarioRepository.save(usuarioExistente);
    }

    private void validarPerfil(String role) {
        if (!Perfil.valido(role)) {
            throw new IllegalArgumentException("Perfil inválido.");
        }
    }

    private String validarUsername(String username, String idAtual) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username é obrigatório.");
        }

        String limpo = username.trim();
        usuarioRepository.findByUsername(limpo).ifPresent(u -> {
            if (!u.getId().equals(idAtual)) {
                throw new IllegalArgumentException("Username já está em uso.");
            }
        });
        return limpo;
    }
}
