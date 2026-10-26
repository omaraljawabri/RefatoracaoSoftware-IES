package com.saas.billing.config;

import com.saas.billing.SubscriptionBillingService;
import com.saas.billing.adapter.email.CommonsEmailNotificationService;
import com.saas.billing.adapter.email.SmtpSettings;
import com.saas.billing.adapter.jdbc.JdbcConnectionFactory;
import com.saas.billing.adapter.jdbc.JdbcCustomerRepository;
import com.saas.billing.adapter.jdbc.JdbcInvoiceRepository;
import com.saas.billing.adapter.payment.TokenPrefixPaymentGateway;

import java.time.Clock;

/**
 * Monta o {@link SubscriptionBillingService} com os adaptadores reais.
 *
 * É o único lugar que conhece ao mesmo tempo o serviço e as implementações
 * de infraestrutura (composition root). Os testes não usam esta classe:
 * montam o serviço com fakes.
 */
public final class BillingServiceFactory {

    private BillingServiceFactory() {
    }

    /** Lê a configuração do banco e do SMTP das variáveis de ambiente e usa o relógio do sistema. */
    public static SubscriptionBillingService fromEnvironment() {
        return create(JdbcConnectionFactory.fromEnvironment(), SmtpSettings.fromEnvironment(),
                Clock.systemDefaultZone());
    }

    static SubscriptionBillingService create(JdbcConnectionFactory connections, SmtpSettings smtp, Clock clock) {
        return new SubscriptionBillingService(
                new JdbcCustomerRepository(connections),
                new JdbcInvoiceRepository(connections),
                new TokenPrefixPaymentGateway(),
                new CommonsEmailNotificationService(smtp),
                clock);
    }
}
