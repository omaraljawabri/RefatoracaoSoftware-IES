package com.saas.billing.fake;

import com.saas.billing.domain.Money;
import com.saas.billing.port.PaymentGateway;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake de {@link PaymentGateway} para testes: aprova ou recusa conforme
 * configurado e registra cada cobrança recebida.
 */
public class FakePaymentGateway implements PaymentGateway {

    /** Uma cobrança recebida pelo fake. */
    public record Charge(String cardToken, Money amount) { }

    private final boolean approve;
    private final List<Charge> charges = new ArrayList<>();

    private FakePaymentGateway(boolean approve) {
        this.approve = approve;
    }

    /** Fake que aprova todos os pagamentos. */
    public static FakePaymentGateway approving() {
        return new FakePaymentGateway(true);
    }

    /** Fake que recusa todos os pagamentos. */
    public static FakePaymentGateway declining() {
        return new FakePaymentGateway(false);
    }

    @Override
    public boolean charge(String cardToken, Money amount) {
        charges.add(new Charge(cardToken, amount));
        return approve;
    }

    /** Cobranças recebidas, na ordem em que chegaram. */
    public List<Charge> charges() {
        return List.copyOf(charges);
    }
}
