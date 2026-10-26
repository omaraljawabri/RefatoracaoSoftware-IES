package com.saas.billing.adapter.payment;

import com.saas.billing.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenPrefixPaymentGatewayTest {

    private final TokenPrefixPaymentGateway gateway = new TokenPrefixPaymentGateway();
    private final Money valor = Money.of("59.50");

    @Test
    @DisplayName("Aprova token que começa com tok_")
    void aprovaTokenValido() {
        assertTrue(gateway.charge("tok_abc123", valor));
    }

    @Test
    @DisplayName("Recusa token nulo")
    void recusaTokenNulo() {
        assertFalse(gateway.charge(null, valor));
    }

    @Test
    @DisplayName("Recusa token vazio ou com outro prefixo")
    void recusaTokenInvalido() {
        assertFalse(gateway.charge("", valor));
        assertFalse(gateway.charge("card_abc123", valor));
        assertFalse(gateway.charge(" tok_abc123", valor));
    }

    @Test
    @DisplayName("Diferencia maiúsculas no prefixo, como o código legado")
    void prefixoDiferenciaMaiusculas() {
        assertFalse(gateway.charge("TOK_abc123", valor));
    }
}
