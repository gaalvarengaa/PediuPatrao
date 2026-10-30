package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public record IndicadoresDashboard(BigDecimal faturamento, long pedidos, BigDecimal ticketMedio,
        long cancelamentos, BigDecimal descontos) {
}
