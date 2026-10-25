package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Dados de entrada de uma cobrança mensal.
 *
 * Substitui os 8 parâmetros soltos de {@code processMonthlyBilling}
 * (Introduce Parameter Object). O parâmetro {@code planType}, que nunca era
 * usado, não faz parte do comando.
 *
 * @param customerId       identificador do cliente
 * @param basePrice        preço base do plano
 * @param signupDate       data de adesão do cliente
 * @param overdueDays      dias de atraso no pagamento (0 se em dia)
 * @param promoCode        código promocional informado, ou {@code null}
 * @param nonProfit        {@code true} se o cliente é uma ONG
 * @param paymentCardToken token do cartão de pagamento, ou {@code null}
 */
public record BillingCommand(
        String customerId,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(basePrice, "basePrice não pode ser nulo");
        Objects.requireNonNull(signupDate, "signupDate não pode ser nulo");
    }

    /**
     * Cria o comando a partir dos valores no formato recebido hoje pelo serviço.
     *
     * A data é convertida com {@link LocalDate#parse(CharSequence)}, exatamente
     * como no código legado: uma data inválida lança
     * {@link java.time.format.DateTimeParseException} e uma data nula lança
     * {@link NullPointerException}. A validação com mensagem clara fica para um
     * commit de correção posterior, porque muda o comportamento atual.
     */
    public static BillingCommand from(String customerId, double basePrice, String signupDateStr,
                                      int overdueDays, String promoCode, boolean nonProfit,
                                      String paymentCardToken) {
        return new BillingCommand(
                customerId,
                Money.of(basePrice),
                LocalDate.parse(signupDateStr),
                overdueDays,
                promoCode,
                nonProfit,
                paymentCardToken);
    }
}