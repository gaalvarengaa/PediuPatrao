package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.AgrupamentoVendas;
import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.VendasDashboard;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardVendasServiceTest {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private DashboardVendasService service;

    @BeforeEach
    void setUp() {
        service = new DashboardVendasService(
                new DashboardConsultaService(pedidoRepository, mock(ProdutoRepository.class)));
    }

    @Test
    void agrupaPorDiaIncluindoDiasSemVendas() {
        comPedidos(pedido(LocalDateTime.of(2026, 10, 1, 12, 0), StatusPedido.FINALIZADO, "10.00"),
                pedido(LocalDateTime.of(2026, 10, 1, 23, 30), StatusPedido.PRONTO, "20.00"),
                pedido(LocalDateTime.of(2026, 10, 3, 0, 0), StatusPedido.RECEBIDO, "5.50"));

        assertThat(calcular("2026-10-01", "2026-10-03", AgrupamentoVendas.DIA)).containsExactly(
                new VendasDashboard.Ponto("2026-10-01", new BigDecimal("30.00"), 2),
                new VendasDashboard.Ponto("2026-10-02", new BigDecimal("0.00"), 0),
                new VendasDashboard.Ponto("2026-10-03", new BigDecimal("5.50"), 1));
    }

    @Test
    void agrupaPorSemanaComInicioNaSegundaFeira() {
        comPedidos(pedido(LocalDateTime.of(2026, 10, 4, 12, 0), StatusPedido.FINALIZADO, "10.00"),
                pedido(LocalDateTime.of(2026, 10, 5, 12, 0), StatusPedido.FINALIZADO, "20.00"));

        assertThat(calcular("2026-10-01", "2026-10-11", AgrupamentoVendas.SEMANA)).containsExactly(
                new VendasDashboard.Ponto("2026-09-28", new BigDecimal("10.00"), 1),
                new VendasDashboard.Ponto("2026-10-05", new BigDecimal("20.00"), 1));
    }

    @Test
    void agrupaPorMes() {
        comPedidos(pedido(LocalDateTime.of(2026, 9, 30, 12, 0), StatusPedido.FINALIZADO, "10.00"),
                pedido(LocalDateTime.of(2026, 11, 1, 12, 0), StatusPedido.FINALIZADO, "20.00"));

        assertThat(calcular("2026-09-30", "2026-11-01", AgrupamentoVendas.MES)).containsExactly(
                new VendasDashboard.Ponto("2026-09", new BigDecimal("10.00"), 1),
                new VendasDashboard.Ponto("2026-10", new BigDecimal("0.00"), 0),
                new VendasDashboard.Ponto("2026-11", new BigDecimal("20.00"), 1));
    }

    @Test
    void naoConsideraPedidosCancelados() {
        comPedidos(pedido(LocalDateTime.of(2026, 10, 1, 12, 0), StatusPedido.CANCELADO, "99.00"));

        assertThat(calcular("2026-10-01", "2026-10-01", AgrupamentoVendas.DIA)).containsExactly(
                new VendasDashboard.Ponto("2026-10-01", new BigDecimal("0.00"), 0));
    }

    private List<VendasDashboard.Ponto> calcular(String de, String ate, AgrupamentoVendas agrupamento) {
        FiltroDashboard filtro = new FiltroDashboard(LocalDate.parse(de), LocalDate.parse(ate), null, null, null);
        return service.calcular(filtro, agrupamento).pontos();
    }

    private void comPedidos(Pedido... pedidos) {
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of(pedidos));
    }

    private Pedido pedido(LocalDateTime entrada, StatusPedido status, String total) {
        Pedido pedido = new Pedido();
        pedido.setDataHoraEntrada(entrada);
        pedido.setStatus(status);
        pedido.setValorTotal(new BigDecimal(total));
        return pedido;
    }
}
