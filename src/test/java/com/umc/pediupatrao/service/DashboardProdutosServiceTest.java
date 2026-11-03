package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.ProdutoVendido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardProdutosServiceTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 15);
    private static final FiltroDashboard SEM_FILTROS = new FiltroDashboard(DIA, DIA, null, null, null);

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private DashboardProdutosService service;

    @BeforeEach
    void setUp() {
        service = new DashboardProdutosService(
                new DashboardConsultaService(pedidoRepository, mock(ProdutoRepository.class)));
    }

    @Test
    void ordenaPorQuantidadeESomaOsItensDeVariosPedidos() {
        comPedidos(pedido(StatusPedido.FINALIZADO, item("p1", "Calabresa", 1, "45.00"), item("p2", "Refrigerante", 2, "12.00")),
                pedido(StatusPedido.RECEBIDO, item("p1", "Calabresa", 2, "45.00")));

        assertThat(service.calcular(SEM_FILTROS, 10)).containsExactly(
                new ProdutoVendido("p1", "Calabresa", 3, new BigDecimal("135.00")),
                new ProdutoVendido("p2", "Refrigerante", 2, new BigDecimal("24.00")));
    }

    @Test
    void desempataPeloMaiorFaturamento() {
        comPedidos(pedido(StatusPedido.FINALIZADO, item("p1", "Borda", 1, "8.00"), item("p2", "Calabresa", 1, "45.00")));

        assertThat(service.calcular(SEM_FILTROS, 10)).extracting(ProdutoVendido::produtoId)
                .containsExactly("p2", "p1");
    }

    @Test
    void respeitaOLimite() {
        comPedidos(pedido(StatusPedido.FINALIZADO, item("p1", "A", 3, "1.00"), item("p2", "B", 2, "1.00"), item("p3", "C", 1, "1.00")));

        assertThat(service.calcular(SEM_FILTROS, 2)).hasSize(2);
    }

    @Test
    void naoConsideraPedidosCancelados() {
        comPedidos(pedido(StatusPedido.CANCELADO, item("p1", "Calabresa", 5, "45.00")));

        assertThat(service.calcular(SEM_FILTROS, 10)).isEmpty();
    }

    @Test
    void comFiltroDeProdutoConsideraApenasOsItensQueAtendem() {
        comPedidos(pedido(StatusPedido.FINALIZADO, item("p1", "Calabresa", 1, "45.00"), item("p2", "Refrigerante", 2, "12.00")));

        assertThat(service.calcular(new FiltroDashboard(DIA, DIA, null, "p2", null), 10)).containsExactly(
                new ProdutoVendido("p2", "Refrigerante", 2, new BigDecimal("24.00")));
    }

    @Test
    void usaONomeMaisRecenteDoProduto() {
        comPedidos(pedido(StatusPedido.FINALIZADO, item("p1", "Nome antigo", 1, "10.00")),
                pedido(StatusPedido.FINALIZADO, item("p1", "Nome novo", 1, "10.00")));

        assertThat(service.calcular(SEM_FILTROS, 10).get(0).nome()).isEqualTo("Nome novo");
    }

    @Test
    void rejeitaLimiteForaDaFaixa() {
        for (int limite : new int[]{0, 51}) {
            assertThatThrownBy(() -> service.calcular(SEM_FILTROS, limite))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("O limite deve estar entre 1 e 50.");
        }
    }

    private void comPedidos(Pedido... pedidos) {
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of(pedidos));
    }

    private Pedido pedido(StatusPedido status, ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setItens(List.of(itens));
        return pedido;
    }

    private ItemPedido item(String produtoId, String nome, int quantidade, String preco) {
        return new ItemPedido(produtoId, nome, quantidade, new BigDecimal(preco));
    }
}
