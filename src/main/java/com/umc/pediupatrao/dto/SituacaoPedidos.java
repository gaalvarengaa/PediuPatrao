package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;

public record SituacaoPedidos(StatusPedido status, String descricao, long quantidade) {
}
