package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DashboardConsultaServiceTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 15);

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    private DashboardConsultaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DashboardConsultaService(pedidoRepository, produtoRepository);
    }

    @Test
    void periodoIncluiODiaInteiroDasDatasInformadas() {
        service.consultar(new FiltroDashboard(DIA, DIA.plusDays(1), null, null, null));

        verify(pedidoRepository).buscarPorPeriodo(LocalDateTime.of(2026, 10, 15, 0, 0),
                LocalDateTime.of(2026, 10, 17, 0, 0));
    }

    @Test
    void semDatasConsideraOsUltimos30Dias() {
        DashboardConsultaService.Consulta consulta = service.consultar(new FiltroDashboard(null, null, null, null, null));

        assertThat(consulta.ate()).isEqualTo(LocalDate.now());
        assertThat(consulta.de()).isEqualTo(LocalDate.now().minusDays(29));
    }

    @Test
    void rejeitaDataInicialPosteriorAFinal() {
        assertThatThrownBy(() -> service.consultar(new FiltroDashboard(DIA, DIA.minusDays(1), null, null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A data inicial não pode ser posterior à data final.");
    }

    @Test
    void filtraPorStatusConsiderandoStatusAusenteComoRecebido() {
        Pedido recebido = pedido(StatusPedido.RECEBIDO, item("p1", null));
        Pedido semStatus = pedido(null, item("p1", null));
        Pedido pronto = pedido(StatusPedido.PRONTO, item("p1", null));
        comPedidos(recebido, semStatus, pronto);

        assertThat(consultar(StatusPedido.RECEBIDO, null, null)).containsExactly(recebido, semStatus);
    }

    @Test
    void filtraPedidosQueContemOProduto() {
        Pedido comProduto = pedido(StatusPedido.RECEBIDO, item("p1", null), item("p2", null));
        Pedido semProduto = pedido(StatusPedido.RECEBIDO, item("p3", null));
        comPedidos(comProduto, semProduto);

        assertThat(consultar(null, "p2", null)).containsExactly(comProduto);
    }

    @Test
    void filtraPorCategoriaCopiadaNoItemSemConsiderarMaiusculas() {
        Pedido pizza = pedido(StatusPedido.RECEBIDO, item("p1", "Pizza"));
        Pedido bebida = pedido(StatusPedido.RECEBIDO, item("p2", "Bebida"));
        comPedidos(pizza, bebida);

        assertThat(consultar(null, null, "pizza")).containsExactly(pizza);
    }

    @Test
    void pedidoAntigoUsaCategoriaDoCadastroEProdutoExcluidoNaoAtende() {
        Produto cadastrado = new Produto();
        cadastrado.setId("p1");
        cadastrado.setTipo("Pizza");
        when(produtoRepository.findAll()).thenReturn(List.of(cadastrado));
        Pedido antigo = pedido(StatusPedido.RECEBIDO, item("p1", null));
        Pedido produtoExcluido = pedido(StatusPedido.RECEBIDO, item("removido", null));
        comPedidos(antigo, produtoExcluido);

        assertThat(consultar(null, null, "Pizza")).containsExactly(antigo);
    }

    private List<Pedido> consultar(StatusPedido status, String produtoId, String tipo) {
        return service.consultar(new FiltroDashboard(DIA, DIA, status, produtoId, tipo)).pedidos();
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

    private ItemPedido item(String produtoId, String tipo) {
        ItemPedido item = new ItemPedido(produtoId, "Produto " + produtoId, 1, BigDecimal.TEN);
        item.setTipoProduto(tipo);
        return item;
    }
}
