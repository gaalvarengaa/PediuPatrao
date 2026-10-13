package com.umc.pediupatrao.entity;

/**
 * Campo modificado em uma operação auditada, com o valor anterior e o novo.
 */
public record CampoAlterado(String campo, String valorAnterior, String valorNovo) {
}
