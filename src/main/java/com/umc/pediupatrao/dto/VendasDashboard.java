package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.util.List;

public record VendasDashboard(AgrupamentoVendas agrupamento, List<Ponto> pontos) {

    public record Ponto(String periodo, BigDecimal faturamento, long pedidos) {
    }
}
