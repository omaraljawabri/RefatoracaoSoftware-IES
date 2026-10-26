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
    @DisplayName("charge: aprova o pagamento quando o token começa com tok_")
    void charge_aprovaPagamento_quandoTokenComecaComTok() {
        assertTrue(gateway.charge("tok_abc123", valor));
    }

    @Test
    @DisplayName("charge: recusa o pagamento quando o token é nulo")
    void charge_recusaPagamento_quandoTokenNulo() {
        assertFalse(gateway.charge(null, valor));
    }

    @Test
    @DisplayName("charge: recusa o pagamento quando o token é vazio ou tem outro prefixo")
    void charge_recusaPagamento_quandoTokenVazioOuComOutroPrefixo() {
        assertFalse(gateway.charge("", valor));
        assertFalse(gateway.charge("card_abc123", valor));
        assertFalse(gateway.charge(" tok_abc123", valor));
    }

    @Test
    @DisplayName("charge: recusa o pagamento quando o prefixo está em maiúsculas (como no legado)")
    void charge_recusaPagamento_quandoPrefixoEmMaiusculas() {
        assertFalse(gateway.charge("TOK_abc123", valor));
    }
}
