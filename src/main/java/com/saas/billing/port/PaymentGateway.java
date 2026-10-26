package com.saas.billing.port;

import com.saas.billing.domain.Money;

/**
 * Porta para o serviço externo de cobrança no cartão.
 *
 * <p>O serviço de cobrança depende só desta interface; a implementação real
 * fica em um adaptador e, nos testes, é trocada por um fake.</p>
 */
public interface PaymentGateway {

    /**
     * Cobra o valor no cartão representado pelo token.
     *
     * @param cardToken token do cartão, ou {@code null} se não informado
     * @param amount    valor a cobrar
     * @return {@code true} se o pagamento foi aprovado
     */
    boolean charge(String cardToken, Money amount);
}
