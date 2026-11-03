package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public record ProdutoVendido(String produtoId, String nome, long quantidade, BigDecimal faturamento) {
}
