package com.umc.pediupatrao.exception;

/**
 * Violação de uma regra de negócio (transição inválida, desconto acima do limite, etc.).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
