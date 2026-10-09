package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            criarSeNaoExistir(repo, encoder, "admin", Perfil.ADMIN);
            criarSeNaoExistir(repo, encoder, "gerente", Perfil.GERENTE);
            criarSeNaoExistir(repo, encoder, "atendente", Perfil.ATENDENTE);
        };
    }

    private void criarSeNaoExistir(UsuarioRepository repo, PasswordEncoder encoder, String username, Perfil perfil) {
        Usuario user = repo.findByUsername(username).orElse(null);

        if (user == null) {
            user = new Usuario();
            user.setUsername(username);
            user.setPassword(encoder.encode("teste123"));
            user.setRole(perfil.name());
            repo.save(user);
            System.out.println("Usuário " + username + " criado!");
        } else if (!Perfil.valido(user.getRole())) {
            user.setRole(perfil.name());
            repo.save(user);
        }
    }
}
