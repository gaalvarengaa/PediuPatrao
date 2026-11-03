package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.SituacaoPedidos;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardSituacaoServiceTest {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final DashboardSituacaoService service = new DashboardSituacaoService(
            new DashboardConsultaService(pedidoRepository, mock(ProdutoRepository.class)));

    @Test
    void contaPedidosDeCadaSituacaoEMantemAsSituacoesSemPedidos() {
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of(
                pedido(StatusPedido.RECEBIDO), pedido(null), pedido(StatusPedido.CANCELADO)));

        List<SituacaoPedidos> situacoes = service.calcular(
                new FiltroDashboard(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, null));

        assertThat(situacoes).hasSize(StatusPedido.values().length);
        assertThat(situacoes).contains(
                new SituacaoPedidos(StatusPedido.RECEBIDO, "Recebido", 2),
                new SituacaoPedidos(StatusPedido.CANCELADO, "Cancelado", 1),
                new SituacaoPedidos(StatusPedido.EM_PREPARACAO, "Em preparação", 0));
    }

    private Pedido pedido(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        return pedido;
    }
}
