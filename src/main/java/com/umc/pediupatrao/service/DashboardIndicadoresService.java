package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.IndicadoresDashboard;
import com.umc.pediupatrao.entity.Pedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class DashboardIndicadoresService {

    private final DashboardConsultaService consultaService;

    public DashboardIndicadoresService(DashboardConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    public IndicadoresDashboard calcular(FiltroDashboard filtro) {
        List<Pedido> todos = consultaService.consultar(filtro).pedidos();
        List<Pedido> validos = todos.stream().filter(DashboardConsultaService::naoCancelado).toList();

        BigDecimal faturamento = somar(validos, Pedido::getValorTotal);
        BigDecimal ticketMedio = validos.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(validos.size()), 2, RoundingMode.HALF_UP);

        return new IndicadoresDashboard(faturamento, validos.size(), ticketMedio,
                todos.size() - validos.size(), somar(validos, Pedido::getValorDesconto));
    }

    private BigDecimal somar(List<Pedido> pedidos, Function<Pedido, BigDecimal> campo) {
        return pedidos.stream()
                .map(campo)
                .map(DashboardConsultaService::valorOuZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
