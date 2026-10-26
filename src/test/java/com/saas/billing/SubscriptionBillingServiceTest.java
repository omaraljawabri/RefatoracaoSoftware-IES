package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.BillingResult;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.CustomerStatus;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.fake.FakePaymentGateway;
import com.saas.billing.fake.InMemoryCustomerRepository;
import com.saas.billing.fake.InMemoryInvoiceRepository;
import com.saas.billing.fake.RecordingNotificationService;
import com.saas.billing.fake.RecordingNotificationService.SentInvoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionBillingServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);
    private static final LocalDate ADESAO_RECENTE = LocalDate.of(2026, 5, 15);
    private static final String CLIENTE = "cus_1";
    private static final String EMAIL = "ana@exemplo.com";

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository()
            .add(new Customer(CLIENTE, EMAIL, CustomerStatus.ACTIVE));
    private final InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
    private final RecordingNotificationService notifications = new RecordingNotificationService();

    private static Clock relogioEm(LocalDate data) {
        return Clock.fixed(data.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    private SubscriptionBillingService servico(FakePaymentGateway payments, LocalDate hoje) {
        return new SubscriptionBillingService(customers, invoices, payments, notifications, relogioEm(hoje));
    }

    private static BillingCommand comando(String cliente, String preco, LocalDate adesao, String cupom) {
        return new BillingCommand(cliente, Money.of(preco), adesao, 0, cupom, false, "tok_visa");
    }

    @Test
    @DisplayName("Pagamento aprovado: cobra, grava fatura PAID, envia o recibo e registra as mensagens")
    void pagamentoAprovado() {
        FakePaymentGateway payments = FakePaymentGateway.approving();

        BillingResult resultado = servico(payments, HOJE)
                .processMonthlyBilling(comando(CLIENTE, "100", ADESAO_RECENTE, null));

        assertEquals(Money.of("100.00"), resultado.amount());
        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 100,00",
                "E-mail disparado para: " + EMAIL), resultado.messages());
        assertEquals(List.of(new FakePaymentGateway.Charge("tok_visa", Money.of("100.00"))), payments.charges());
        assertEquals(List.of(new Invoice(CLIENTE, Money.of("100.00"), InvoiceStatus.PAID)), invoices.saved());
        assertEquals(List.of(new SentInvoice(EMAIL, Money.of("100.00"))), notifications.sent());
    }

    @Test
    @DisplayName("Pagamento recusado: grava fatura FAILED e ainda envia o recibo, como o código legado")
    void pagamentoRecusado() {
        BillingResult resultado = servico(FakePaymentGateway.declining(), HOJE)
                .processMonthlyBilling(comando(CLIENTE, "100", ADESAO_RECENTE, null));

        assertEquals(List.of(
                "Falha no pagamento do cartão para: " + CLIENTE,
                "E-mail disparado para: " + EMAIL), resultado.messages());
        assertEquals(List.of(new Invoice(CLIENTE, Money.of("100.00"), InvoiceStatus.FAILED)), invoices.saved());
        assertEquals(List.of(new SentInvoice(EMAIL, Money.of("100.00"))), notifications.sent());
    }

    @Test
    @DisplayName("Cliente inativo: cancela sem cobrar, sem fatura e sem recibo, mas devolve o valor calculado")
    void clienteInativo() {
        customers.add(new Customer("cus_2", "bia@exemplo.com", CustomerStatus.INACTIVE));
        FakePaymentGateway payments = FakePaymentGateway.approving();

        BillingResult resultado = servico(payments, HOJE)
                .processMonthlyBilling(comando("cus_2", "100", ADESAO_RECENTE, null));

        assertEquals(Money.of("100.00"), resultado.amount());
        assertEquals(List.of("Cliente inativo. Cobrança cancelada: cus_2"), resultado.messages());
        assertTrue(payments.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    @DisplayName("Cliente inexistente: não faz nada e não gera nenhuma mensagem, como o código legado")
    void clienteInexistente() {
        FakePaymentGateway payments = FakePaymentGateway.approving();

        BillingResult resultado = servico(payments, HOJE)
                .processMonthlyBilling(comando("cus_desconhecido", "100", ADESAO_RECENTE, null));

        assertEquals(Money.of("100.00"), resultado.amount());
        assertTrue(resultado.messages().isEmpty());
        assertTrue(payments.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }
}
