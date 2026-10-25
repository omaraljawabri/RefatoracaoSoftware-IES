package com.saas.billing.domain;

import java.util.Objects;

/**
 * Fatura gerada por uma cobrança, com os dados que o código legado gravava
 * na tabela {@code invoices}.
 *
 * @param customerId identificador do cliente cobrado
 * @param amount     valor cobrado
 * @param status     resultado do pagamento
 */
public record Invoice(String customerId, Money amount, InvoiceStatus status) {

    public Invoice {
        Objects.requireNonNull(customerId, "customerId não pode ser nulo");
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        Objects.requireNonNull(status, "status não pode ser nulo");
    }
}
