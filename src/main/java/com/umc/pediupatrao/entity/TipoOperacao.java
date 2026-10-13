package com.umc.pediupatrao.entity;

public enum TipoOperacao {
    INCLUSAO_PEDIDO("INCLUSÃO DE PEDIDO"),
    ALTERACAO_STATUS_PEDIDO("ALTERAÇÃO DE STATUS DO PEDIDO"),
    DESCONTO_PEDIDO("DESCONTO EM PEDIDO"),
    CANCELAMENTO_PEDIDO("CANCELAMENTO DE PEDIDO"),
    INCLUSAO_CLIENTE("INCLUSÃO DE CLIENTE"),
    ALTERACAO_CLIENTE("ALTERAÇÃO DE CLIENTE"),
    EXCLUSAO_CLIENTE("EXCLUSÃO DE CLIENTE");

    private final String descricao;

    TipoOperacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
