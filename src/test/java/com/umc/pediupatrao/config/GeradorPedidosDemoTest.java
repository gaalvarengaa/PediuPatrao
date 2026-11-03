package com.umc.pediupatrao.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GeradorPedidosDemoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 3, 21, 0);

    private final List<Pedido> pedidos = new GeradorPedidosDemo(42L).gerar(produtos(), clientes(), AGORA);

    @Test
    void geraPedidosEmTodosOsDiasDoPeriodoSemPassarDeAgora() {
        Set<LocalDate> dias = pedidos.stream().map(p -> p.getDataHoraEntrada().toLocalDate()).collect(Collectors.toSet());

        assertThat(pedidos).hasSizeGreaterThan(GeradorPedidosDemo.DIAS * 4);
        assertThat(dias).hasSize(GeradorPedidosDemo.DIAS);
        assertThat(pedidos).allSatisfy(p -> assertThat(p.getDataHoraEntrada()).isBeforeOrEqualTo(AGORA));
    }

    @Test
    void valoresBatemComOsItensEODesconto() {
        assertThat(pedidos).allSatisfy(p -> {
            BigDecimal somaItens = p.getItens().stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(p.getItens()).isNotEmpty();
            assertThat(p.getSubtotal()).isEqualByComparingTo(somaItens);
            assertThat(p.getValorTotal()).isEqualByComparingTo(p.getSubtotal().subtract(p.getValorDesconto()));
            assertThat(p.getItens()).allSatisfy(i -> assertThat(i.getTipoProduto()).isNotBlank());
        });
        assertThat(pedidos).anySatisfy(p -> assertThat(p.getValorDesconto()).isPositive());
    }

    @Test
    void diasAnterioresTerminamFinalizadosOuCancelados() {
        Set<StatusPedido> statusAnteriores = pedidos.stream()
                .filter(p -> p.getDataHoraEntrada().toLocalDate().isBefore(AGORA.toLocalDate()))
                .map(Pedido::getStatus)
                .collect(Collectors.toSet());

        assertThat(statusAnteriores).containsExactlyInAnyOrder(StatusPedido.FINALIZADO, StatusPedido.CANCELADO);
    }

    @Test
    void pedidosDeHojeFicamEmAndamento() {
        Set<StatusPedido> statusHoje = pedidos.stream()
                .filter(p -> p.getDataHoraEntrada().toLocalDate().equals(AGORA.toLocalDate()))
                .map(Pedido::getStatus)
                .collect(Collectors.toSet());

        assertThat(statusHoje).containsAnyElementsOf(EnumSet.of(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO,
                StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO));
    }

    @Test
    void registraResponsaveisEHorariosDeCadaEtapa() {
        assertThat(pedidos).allSatisfy(p -> {
            assertThat(p.getResponsavelEntrada()).isNotBlank();
            if (p.getStatus() == StatusPedido.CANCELADO) {
                assertThat(p.getCanceladoPor()).isNotBlank();
                assertThat(p.getJustificativaCancelamento()).isNotBlank();
                assertThat(p.getDataHoraCancelamento()).isAfter(p.getDataHoraEntrada());
            }
            if (p.getStatus().isSaida() || p.getStatus() == StatusPedido.FINALIZADO) {
                assertThat(p.getDataHoraSaida()).isAfter(p.getDataHoraEntrada());
                assertThat(p.getResponsavelSaida()).isNotBlank();
            }
            if (p.getStatus() == StatusPedido.FINALIZADO) {
                assertThat(p.getDataHoraFinalizacao()).isAfterOrEqualTo(p.getDataHoraSaida());
            }
        });
    }

    @Test
    void mesmaSementeGeraOsMesmosPedidos() {
        List<Pedido> outros = new GeradorPedidosDemo(42L).gerar(produtos(), clientes(), AGORA);

        assertThat(outros).hasSameSizeAs(pedidos);
        assertThat(outros.get(10).getValorTotal()).isEqualByComparingTo(pedidos.get(10).getValorTotal());
    }

    @Test
    void semProdutosAtivosOuClientesNaoGeraNada() {
        Produto inativo = produto("p1", "Pizza Calabresa", "Pizza", 45.0);
        inativo.setAtivo(false);

        assertThat(new GeradorPedidosDemo(1L).gerar(List.of(inativo), clientes(), AGORA)).isEmpty();
        assertThat(new GeradorPedidosDemo(1L).gerar(produtos(), List.of(), AGORA)).isEmpty();
    }

    private static List<Produto> produtos() {
        return List.of(
                produto("p1", "Pizza Calabresa", "Pizza", 45.0),
                produto("p2", "Pizza Margherita", "Pizza", 42.0),
                produto("p3", "Refrigerante 2L", "Bebida", 12.0),
                produto("p4", "Borda Recheada", "Adicional", 8.0));
    }

    private static Produto produto(String id, String nome, String tipo, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }

    private static List<Cliente> clientes() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Ana Souza");
        return List.of(cliente);
    }
}
