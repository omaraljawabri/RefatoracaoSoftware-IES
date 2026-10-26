package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.BillingResult;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PricingCalculator;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentGateway payments;
    private final NotificationService notifications;
    private final Clock clock;
    private final PricingCalculator pricing = new PricingCalculator();

    public SubscriptionBillingService(CustomerRepository customers, InvoiceRepository invoices,
                                      PaymentGateway payments, NotificationService notifications,
                                      Clock clock) {
        this.customers = Objects.requireNonNull(customers, "customers não pode ser nulo");
        this.invoices = Objects.requireNonNull(invoices, "invoices não pode ser nulo");
        this.payments = Objects.requireNonNull(payments, "payments não pode ser nulo");
        this.notifications = Objects.requireNonNull(notifications, "notifications não pode ser nulo");
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    public BillingResult processMonthlyBilling(BillingCommand command) {

        List<String> messages = new ArrayList<>();

        Money amount = pricing.calculate(command, LocalDate.now(clock));

        try {
            Optional<Customer> customer = customers.findById(command.customerId());

            if (customer.isPresent()) {
                String email = customer.get().email();

                if (!customer.get().isActive()) {
                    messages.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                    return new BillingResult(amount, messages);
                }

                boolean paymentSuccess = charge(command, amount, messages);

                invoices.save(new Invoice(command.customerId(), amount, InvoiceStatus.of(paymentSuccess)));

                notifications.sendInvoice(email, amount);
                messages.add("E-mail disparado para: " + email);
            }
        } catch (Exception e) {
            messages.add("Erro fatal no processamento: " + e.getMessage());
        }

        return new BillingResult(amount, messages);
    }

    /** Cobra no cartão e registra a mensagem de sucesso ou de falha do pagamento. */
    private boolean charge(BillingCommand command, Money amount, List<String> messages) {
        boolean paymentSuccess = payments.charge(command.paymentCardToken(), amount);
        if (paymentSuccess) {
            messages.add("Pagamento processado com sucesso: " + amount.formatted());
        } else {
            messages.add("Falha no pagamento do cartão para: " + command.customerId());
        }
        return paymentSuccess;
    }
}
