package com.saas.billing.fake;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake de {@link NotificationService} para testes: não envia nada, só
 * registra as notificações recebidas.
 */
public class RecordingNotificationService implements NotificationService {

    /** Uma notificação de fatura registrada pelo fake. */
    public record SentInvoice(String email, Money amount) { }

    private final List<SentInvoice> sent = new ArrayList<>();

    @Override
    public void sendInvoice(String email, Money amount) {
        sent.add(new SentInvoice(email, amount));
    }

    /** Notificações registradas, na ordem em que chegaram. */
    public List<SentInvoice> sent() {
        return List.copyOf(sent);
    }
}
