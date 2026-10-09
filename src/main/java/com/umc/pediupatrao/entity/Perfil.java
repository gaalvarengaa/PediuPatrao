package com.umc.pediupatrao.entity;

public enum Perfil {
    ADMIN,
    GERENTE,
    ATENDENTE;

    public static boolean valido(String valor) {
        if (valor == null) {
            return false;
        }
        for (Perfil p : values()) {
            if (p.name().equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
