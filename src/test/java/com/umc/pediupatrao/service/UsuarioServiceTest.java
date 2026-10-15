package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Regras de administração segura dos perfis (proteção contra autoelevação e autoalteração).
 */
class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash-" + i.getArgument(0));
    }

    @Test
    void naoPodeAlterarOProprioPerfil() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "admin", "ADMIN")));

        assertThatThrownBy(() -> usuarioService.atualizarUsuario("u1", dados("admin", "ATENDENTE"), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("próprio perfil");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoPodeRenomearAPropriaConta() {
        // Renomear a própria conta permitiria escapar das verificações feitas pelo username da sessão
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "admin", "ADMIN")));
        when(usuarioRepository.findByUsername("admin2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizarUsuario("u1", dados("admin2", "ADMIN"), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("próprio username");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoPodeExcluirAPropriaConta() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario("u1", "admin", "ADMIN")));

        assertThatThrownBy(() -> usuarioService.deletarUsuario("u1", "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepository, never()).deleteById(anyString());
    }

    @Test
    void adminAlteraPerfilDeOutroUsuario() {
        when(usuarioRepository.findById("u2")).thenReturn(Optional.of(usuario("u2", "joao", "ATENDENTE")));
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario("u2", "joao", "ATENDENTE")));

        Usuario atualizado = usuarioService.atualizarUsuario("u2", dados("joao", "GERENTE"), "admin");

        assertThat(atualizado.getRole()).isEqualTo("GERENTE");
    }

    @Test
    void perfilInexistenteEhRejeitado() {
        assertThatThrownBy(() -> usuarioService.salvarUsuario(dados("novo", "ROOT", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Perfil inválido");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaEhGravadaCriptografada() {
        when(usuarioRepository.findByUsername("novo")).thenReturn(Optional.empty());

        Usuario salvo = usuarioService.salvarUsuario(dados("novo", "ATENDENTE", "123456"));

        assertThat(salvo.getPassword()).isEqualTo("hash-123456");
    }

    private Usuario usuario(String id, String username, String role) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setRole(role);
        usuario.setPassword("hash-antigo");
        return usuario;
    }

    private Usuario dados(String username, String role) {
        return dados(username, role, null);
    }

    private Usuario dados(String username, String role, String senha) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setRole(role);
        usuario.setPassword(senha);
        return usuario;
    }
}
