package com.umc.pediupatrao.entity;

import java.util.List;

/**
 * Fluxo obrigatório do pedido:
 * RECEBIDO -> EM_PREPARACAO -> PRONTO -> (SAIU_PARA_ENTREGA | RETIRADO) -> FINALIZADO.
 * CANCELADO é excepcional e só é atingido pela operação de cancelamento do gerente.
 */
public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Próximos status alcançáveis pela atualização normal de status (sem cancelamento). */
    public List<StatusPedido> getProximosStatus() {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> List.of(SAIU_PARA_ENTREGA, RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    public boolean podeIrPara(StatusPedido novo) {
        return getProximosStatus().contains(novo);
    }

    /** Status que representam a saída do pedido da pizzaria. */
    public boolean isSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    /** Cancelamento e desconto só são permitidos antes da saída. */
    public boolean isAntesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    /** Converte valores gravados pela versão anterior do sistema (status em texto livre). */
    public static StatusPedido deValorLegado(String valor) {
        if (valor == null || valor.isBlank()) {
            return RECEBIDO;
        }
        String normalizado = valor.trim().toUpperCase();
        for (StatusPedido s : values()) {
            if (s.name().equals(normalizado)) {
                return s;
            }
        }
        return switch (normalizado) {
            case "EM_PREPARO" -> EM_PREPARACAO;
            case "ENTREGUE" -> FINALIZADO;
            default -> RECEBIDO;
        };
    }
}
