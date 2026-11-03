package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final long SEMENTE = 42L;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public DemoDataInitializer(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void carregar() {
        if (pedidoRepository.count() > 0) {
            log.info("Perfil demo: já existem pedidos, nenhum pedido de exemplo foi criado.");
            return;
        }
        criarProdutosDemo();
        criarClientesDemo();
        List<Pedido> pedidos = new GeradorPedidosDemo(SEMENTE)
                .gerar(produtoRepository.findAll(), clienteRepository.findAll(), LocalDateTime.now());
        pedidoRepository.saveAll(pedidos);
        log.info("Perfil demo: {} pedidos de exemplo criados nos últimos {} dias.", pedidos.size(), GeradorPedidosDemo.DIAS);
    }

    private void criarProdutosDemo() {
        Set<String> existentes = produtoRepository.findAll().stream().map(Produto::getNome).collect(Collectors.toSet());
        List.of(
                produto("Pizza Calabresa", "Pizza", 45.00),
                produto("Pizza Margherita", "Pizza", 42.00),
                produto("Pizza Quatro Queijos", "Pizza", 52.00),
                produto("Pizza Portuguesa", "Pizza", 48.00),
                produto("Pizza Frango com Catupiry", "Pizza", 50.00),
                produto("Pizza de Chocolate", "Doce", 46.00),
                produto("Refrigerante 2L", "Bebida", 12.00),
                produto("Refrigerante Lata", "Bebida", 6.00),
                produto("Suco Natural", "Bebida", 9.00),
                produto("Borda Recheada", "Adicional", 8.00))
                .stream()
                .filter(p -> !existentes.contains(p.getNome()))
                .forEach(produtoRepository::save);
    }

    private void criarClientesDemo() {
        if (clienteRepository.count() > 0) {
            return;
        }
        List.of("Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Alves", "Eduarda Rocha",
                "Felipe Costa", "Gabriela Nunes", "Henrique Dias", "Isabela Martins", "João Pereira")
                .forEach(nome -> {
                    Cliente cliente = new Cliente();
                    cliente.setNome(nome);
                    cliente.setTelefone(String.format("(11) 9%04d-%04d", Math.abs(nome.hashCode()) % 10000,
                            Math.abs(nome.hashCode() / 10000) % 10000));
                    clienteRepository.save(cliente);
                });
    }

    private Produto produto(String nome, String tipo, double preco) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
