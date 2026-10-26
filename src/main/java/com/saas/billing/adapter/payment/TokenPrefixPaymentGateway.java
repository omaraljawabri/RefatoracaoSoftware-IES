package com.saas.billing.adapter.payment;

import com.saas.billing.domain.Money;
import com.saas.billing.port.PaymentGateway;

/**
 * Adaptador de pagamento com a mesma regra do código legado (linhas 74–76):
 * o pagamento é aprovado quando o token existe e começa com {@code "tok_"}.
 *
 * <p>Nenhuma cobrança real é feita. Quando houver um provedor de pagamento de
 * verdade, basta criar outro adaptador para {@link PaymentGateway}, sem mexer
 * no serviço de cobrança.</p>
 */
public class TokenPrefixPaymentGateway implements PaymentGateway {

    private static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public boolean charge(String cardToken, Money amount) {
        return cardToken != null && cardToken.startsWith(VALID_TOKEN_PREFIX);
    }
}
