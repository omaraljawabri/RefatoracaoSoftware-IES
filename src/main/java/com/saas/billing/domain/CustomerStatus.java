package com.saas.billing.domain;

/**
 * Situação cadastral de um cliente.
 *
 * Substitui a comparação de textos {@code "ACTIVE".equalsIgnoreCase(status)}
 * do código legado.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE;

    /**
     * Interpreta o status gravado no banco do mesmo jeito que o código legado:
     * só {@code "ACTIVE"}, sem diferenciar maiúsculas de minúsculas, é ativo.
     * Qualquer outro valor, inclusive {@code null}, é tratado como inativo.
     */
    public static CustomerStatus from(String value) {
        return ACTIVE.name().equalsIgnoreCase(value) ? ACTIVE : INACTIVE;
    }
}
