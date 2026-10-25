package com.saas.billing.domain;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de uma cobrança mensal.
 *
 * Substitui o {@code List<String>} devolvido pelo código legado, que
 * misturava o resultado com mensagens de log. Por enquanto guarda o valor
 * calculado e as mesmas mensagens de hoje, para não mudar o comportamento;
 * um status estruturado da cobrança entra num commit de correção posterior.
 *
 * @param amount   valor final calculado para a cobrança
 * @param messages mensagens geradas durante o processamento, na ordem em que ocorreram
 */
public record BillingResult(Money amount, List<String> messages) {

    /** Garante que o resultado não mude depois de criado. */
    public BillingResult {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        messages = List.copyOf(Objects.requireNonNull(messages, "messages não pode ser nulo"));
    }
}