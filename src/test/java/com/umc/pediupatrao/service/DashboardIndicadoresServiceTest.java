package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.IndicadoresDashboard;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardIndicadoresServiceTest {

    private static final FiltroDashboard FILTRO =
            new FiltroDashboard(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, null);

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private DashboardIndicadoresService service;

    @BeforeEach
    void setUp() {
        service = new DashboardIndicadoresService(
                new DashboardConsultaService(pedidoRepository, mock(ProdutoRepository.class)));
    }

    @Test
    void calculaIndicadoresSemConsiderarCancelados() {
        comPedidos(pedido(StatusPedido.FINALIZADO, "40.00", "5.00"),
                pedido(StatusPedido.RECEBIDO, "60.00", "0.00"),
                pedido(StatusPedido.CANCELADO, "500.00", "50.00"));

        IndicadoresDashboard indicadores = service.calcular(FILTRO);

        assertThat(indicadores.faturamento()).isEqualByComparingTo("100.00");
        assertThat(indicadores.pedidos()).isEqualTo(2);
        assertThat(indicadores.ticketMedio()).isEqualByComparingTo("50.00");
        assertThat(indicadores.cancelamentos()).isEqualTo(1);
        assertThat(indicadores.descontos()).isEqualByComparingTo("5.00");
    }

    @Test
    void arredondaTicketMedioParaDuasCasas() {
        comPedidos(pedido(StatusPedido.RECEBIDO, "50.00", "0"),
                pedido(StatusPedido.RECEBIDO, "50.00", "0"),
                pedido(StatusPedido.RECEBIDO, "25.01", "0"));

        assertThat(service.calcular(FILTRO).ticketMedio()).isEqualTo(new BigDecimal("41.67"));
    }

    @Test
    void semPedidosRetornaValoresZerados() {
        comPedidos();

        IndicadoresDashboard indicadores = service.calcular(FILTRO);

        assertThat(indicadores.faturamento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(indicadores.ticketMedio()).isEqualTo(new BigDecimal("0.00"));
        assertThat(indicadores.descontos()).isEqualTo(new BigDecimal("0.00"));
        assertThat(indicadores.pedidos()).isZero();
        assertThat(indicadores.cancelamentos()).isZero();
    }

    @Test
    void pedidoAntigoSemValoresContaComoZero() {
        Pedido antigo = new Pedido();
        antigo.setValorTotal(null);
        antigo.setValorDesconto(null);
        comPedidos(antigo, pedido(StatusPedido.PRONTO, "30.00", "0"));

        IndicadoresDashboard indicadores = service.calcular(FILTRO);

        assertThat(indicadores.faturamento()).isEqualByComparingTo("30.00");
        assertThat(indicadores.pedidos()).isEqualTo(2);
    }

    private void comPedidos(Pedido... pedidos) {
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of(pedidos));
    }

    private Pedido pedido(StatusPedido status, String total, String desconto) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setValorTotal(new BigDecimal(total));
        pedido.setValorDesconto(new BigDecimal(desconto));
        return pedido;
    }
}
