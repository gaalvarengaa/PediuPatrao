package com.umc.pediupatrao.dto;

import java.util.List;

public record NovoPedidoRequest(String clienteId, List<ItemPedidoRequest> itens) {
}
