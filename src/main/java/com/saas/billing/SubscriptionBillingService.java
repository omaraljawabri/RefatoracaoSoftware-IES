package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.BillingResult;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PromoCode;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/saas_db";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123";

    private final CustomerRepository customers;
    private final PaymentGateway payments;
    private final NotificationService notifications;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customers, PaymentGateway payments,
                                      NotificationService notifications, Clock clock) {
        this.customers = Objects.requireNonNull(customers, "customers não pode ser nulo");
        this.payments = Objects.requireNonNull(payments, "payments não pode ser nulo");
        this.notifications = Objects.requireNonNull(notifications, "notifications não pode ser nulo");
        this.clock = Objects.requireNonNull(clock, "clock não pode ser nulo");
    }

    public BillingResult processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        Money basePrice = command.basePrice();
        Money calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice.minus(basePrice.times(new BigDecimal("0.15"))); // 15% após 2 anos
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice.minus(basePrice.times(new BigDecimal("0.10"))); // 10% após 1 ano
        }

        if (command.nonProfit()) {
            calculatedPrice = calculatedPrice.times(new BigDecimal("0.70")); // 30% off para ONGs
        }

        PromoCode promoCode = PromoCode.from(command.promoCode()).orElse(null);
        if (promoCode == PromoCode.BLACKFRIDAY && today.getMonthValue() == 11) {
            calculatedPrice = calculatedPrice.minus(Money.of("30.00"));
        } else if (promoCode == PromoCode.WELCOME10 && monthsActive <= 1) {
            calculatedPrice = calculatedPrice.times(new BigDecimal("0.90"));
        }

        if (command.overdueDays() > 0) {
            Money penalty = calculatedPrice.times(new BigDecimal("0.02"));
            Money dailyInterest = calculatedPrice.times(
                    new BigDecimal("0.001").multiply(BigDecimal.valueOf(command.overdueDays())));
            calculatedPrice = calculatedPrice.plus(penalty).plus(dailyInterest);
        }

        calculatedPrice = calculatedPrice.atLeastZero();

        try {
            Optional<Customer> customer = customers.findById(command.customerId());

            if (customer.isPresent()) {
                String email = customer.get().email();

                if (!customer.get().isActive()) {
                    logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                    return new BillingResult(calculatedPrice, logs);
                }

                boolean paymentSuccess = payments.charge(command.paymentCardToken(), calculatedPrice);
                if (paymentSuccess) {
                    logs.add("Pagamento processado com sucesso: " + calculatedPrice.formatted());
                } else {
                    logs.add("Falha no pagamento do cartão para: " + command.customerId());
                }

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    String sqlInvoice = "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";
                    PreparedStatement stmtInvoice = conn.prepareStatement(sqlInvoice);
                    stmtInvoice.setString(1, command.customerId());
                    stmtInvoice.setBigDecimal(2, calculatedPrice.amount());
                    stmtInvoice.setString(3, InvoiceStatus.of(paymentSuccess).name());
                    stmtInvoice.executeUpdate();
                }

                notifications.sendInvoice(email, calculatedPrice);
                logs.add("E-mail disparado para: " + email);
            }
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return new BillingResult(calculatedPrice, logs);
    }
}
