package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, ProdutoRepository produtoRepo, PasswordEncoder encoder) {
        return args -> {
            criarSeNaoExistir(repo, encoder, "admin", Perfil.ADMIN);
            criarSeNaoExistir(repo, encoder, "gerente", Perfil.GERENTE);
            criarSeNaoExistir(repo, encoder, "atendente", Perfil.ATENDENTE);
            criarProdutosIniciais(produtoRepo);
        };
    }

    // Cardápio básico para permitir registrar pedidos logo na primeira execução.
    // Só é criado quando não há nenhum produto cadastrado.
    private void criarProdutosIniciais(ProdutoRepository repo) {
        if (repo.count() > 0) {
            return;
        }
        repo.save(produto("Pizza Calabresa", "Pizza", 45.00));
        repo.save(produto("Pizza Margherita", "Pizza", 42.00));
        repo.save(produto("Pizza Quatro Queijos", "Pizza", 52.00));
        repo.save(produto("Refrigerante 2L", "Bebida", 12.00));
        repo.save(produto("Borda Recheada", "Adicional", 8.00));
        System.out.println("Produtos iniciais criados!");
    }

    private Produto produto(String nome, String tipo, double preco) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
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
