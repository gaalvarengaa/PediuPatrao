package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.AgrupamentoVendas;
import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.VendasDashboard;
import com.umc.pediupatrao.entity.Pedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardVendasService {

    private final DashboardConsultaService consultaService;

    public DashboardVendasService(DashboardConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    public VendasDashboard calcular(FiltroDashboard filtro, AgrupamentoVendas agrupamento) {
        DashboardConsultaService.Consulta consulta = consultaService.consultar(filtro);

        // Todos os intervalos entram zerados, para que os dias sem venda apareçam no gráfico
        Map<String, Acumulado> intervalos = new LinkedHashMap<>();
        for (LocalDate dia = consulta.de(); !dia.isAfter(consulta.ate()); dia = dia.plusDays(1)) {
            intervalos.computeIfAbsent(chave(dia, agrupamento), k -> new Acumulado());
        }

        for (Pedido pedido : consulta.pedidos()) {
            if (DashboardConsultaService.naoCancelado(pedido)) {
                Acumulado acumulado = intervalos.get(chave(pedido.getDataHoraEntrada().toLocalDate(), agrupamento));
                acumulado.faturamento = acumulado.faturamento
                        .add(DashboardConsultaService.valorOuZero(pedido.getValorTotal()));
                acumulado.pedidos++;
            }
        }

        List<VendasDashboard.Ponto> pontos = intervalos.entrySet().stream()
                .map(e -> new VendasDashboard.Ponto(e.getKey(),
                        e.getValue().faturamento.setScale(2, RoundingMode.HALF_UP), e.getValue().pedidos))
                .toList();
        return new VendasDashboard(agrupamento, pontos);
    }

    private String chave(LocalDate dia, AgrupamentoVendas agrupamento) {
        return switch (agrupamento) {
            case DIA -> dia.toString();
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
            case MES -> YearMonth.from(dia).toString();
        };
    }

    private static class Acumulado {
        private BigDecimal faturamento = BigDecimal.ZERO;
        private long pedidos;
    }
}
