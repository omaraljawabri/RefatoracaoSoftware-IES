package com.saas.billing.adapter.email;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.exception.InfrastructureException;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.util.Objects;

/**
 * Adaptador de notificação por e-mail com Apache Commons Email.
 *
 * <p>Monta a mesma mensagem do código legado (linhas 89–95), com a
 * configuração do servidor vinda de {@link SmtpSettings}.</p>
 *
 * <p><b>O e-mail ainda não é enviado</b>, porque no legado a chamada a
 * {@code send()} estava comentada. Ativar o envio muda o comportamento do
 * sistema e fica para um commit de correção posterior.</p>
 */
public class CommonsEmailNotificationService implements NotificationService {

    static final String INVOICE_SUBJECT = "Sua Fatura Mensal";

    private final SmtpSettings settings;

    public CommonsEmailNotificationService(SmtpSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings não pode ser nulo");
    }

    @Override
    public void sendInvoice(String email, Money amount) {
        try {
            buildInvoiceEmail(email, amount);
        } catch (EmailException e) {
            throw new InfrastructureException(e.getMessage(), e);
        }
    }

    /** Monta o e-mail da fatura sem enviá-lo. Visível no pacote para os testes. */
    SimpleEmail buildInvoiceEmail(String email, Money amount) throws EmailException {
        SimpleEmail message = new SimpleEmail();
        message.setHostName(settings.host());
        message.setSmtpPort(settings.port());
        message.setFrom(settings.from());
        message.setSubject(INVOICE_SUBJECT);
        message.setMsg(invoiceText(amount));
        message.addTo(email);
        return message;
    }

    static String invoiceText(Money amount) {
        return "Olá! Sua fatura foi processada no valor de " + amount.formatted();
    }
}
