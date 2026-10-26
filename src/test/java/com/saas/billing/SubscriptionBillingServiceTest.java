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
import com.saas.billing.port.exception.InfrastructureException;
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

    @Test
    @DisplayName("BLACKFRIDAY vale só em novembro, conforme a data do Clock")
    void blackFridayDependeDoRelogio() {
        BillingCommand comando = comando(CLIENTE, "100", ADESAO_RECENTE, "BLACKFRIDAY");

        assertEquals(Money.of("70.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 11, 20))
                .processMonthlyBilling(comando).amount());
        assertEquals(Money.of("100.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 10, 31))
                .processMonthlyBilling(comando).amount());
    }

    @Test
    @DisplayName("Desconto de fidelidade usa a data do Clock para contar os meses de assinatura")
    void fidelidadeDependeDoRelogio() {
        BillingCommand comando = comando(CLIENTE, "100", LocalDate.of(2024, 9, 15), null);

        assertEquals(Money.of("90.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 9, 15))
                .processMonthlyBilling(comando).amount());
        assertEquals(Money.of("85.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 10, 15))
                .processMonthlyBilling(comando).amount());
    }

    @Test
    @DisplayName("O valor cobrado, gravado e enviado é o mesmo valor calculado com descontos")
    void valorComDescontoChegaAsPortas() {
        FakePaymentGateway payments = FakePaymentGateway.approving();
        BillingCommand comando = new BillingCommand(CLIENTE, Money.of("100"), LocalDate.of(2024, 9, 15),
                0, null, true, "tok_visa");

        BillingResult resultado = servico(payments, HOJE).processMonthlyBilling(comando);

        assertEquals(Money.of("59.50"), resultado.amount());
        assertEquals(Money.of("59.50"), payments.charges().get(0).amount());
        assertEquals(Money.of("59.50"), invoices.saved().get(0).amount());
        assertEquals(Money.of("59.50"), notifications.sent().get(0).amount());
        assertEquals("Pagamento processado com sucesso: R$ 59,50", resultado.messages().get(0));
    }

    @Test
    @DisplayName("Falha do banco ao gravar a fatura vira mensagem de erro, como o código legado (catch genérico)")
    void falhaDeInfraestruturaViraMensagem() {
        SubscriptionBillingService servico = new SubscriptionBillingService(customers,
                fatura -> {
                    throw new InfrastructureException("Banco fora do ar", new RuntimeException());
                },
                FakePaymentGateway.approving(), notifications, relogioEm(HOJE));

        BillingResult resultado = servico.processMonthlyBilling(comando(CLIENTE, "100", ADESAO_RECENTE, null));

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 100,00",
                "Erro fatal no processamento: Banco fora do ar"), resultado.messages());
        assertTrue(notifications.sent().isEmpty());
    }
}
