package com.saas.billing.domain;

/**
 * Situação de uma fatura gravada.
 *
 * Substitui os textos {@code "PAID"} e {@code "FAILED"} do código legado.
 */
public enum InvoiceStatus {
    PAID,
    FAILED;

    /** Devolve {@code PAID} se o pagamento foi aprovado e {@code FAILED} caso contrário. */
    public static InvoiceStatus of(boolean paymentSuccess) {
        return paymentSuccess ? PAID : FAILED;
    }
}
