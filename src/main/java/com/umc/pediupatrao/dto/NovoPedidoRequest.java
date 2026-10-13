package com.umc.pediupatrao.dto;

import java.util.List;

/**
 * Dados aceitos na criação do pedido. Preços, totais, status e responsável
 * não fazem parte do contrato: são definidos pelo servidor.
 */
public record NovoPedidoRequest(String clienteId, List<ItemPedidoRequest> itens) {
}
