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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("processMonthlyBilling: cobra, grava fatura PAID e envia o recibo quando o pagamento é aprovado")
    void processMonthlyBilling_cobraGravaPaidEEnviaRecibo_quandoPagamentoAprovado() {
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
    @DisplayName("processMonthlyBilling: grava fatura FAILED e ainda envia o recibo quando o pagamento é recusado (como no legado)")
    void processMonthlyBilling_gravaFailedEEnviaRecibo_quandoPagamentoRecusado() {
        BillingResult resultado = servico(FakePaymentGateway.declining(), HOJE)
                .processMonthlyBilling(comando(CLIENTE, "100", ADESAO_RECENTE, null));

        assertEquals(List.of(
                "Falha no pagamento do cartão para: " + CLIENTE,
                "E-mail disparado para: " + EMAIL), resultado.messages());
        assertEquals(List.of(new Invoice(CLIENTE, Money.of("100.00"), InvoiceStatus.FAILED)), invoices.saved());
        assertEquals(List.of(new SentInvoice(EMAIL, Money.of("100.00"))), notifications.sent());
    }

    @Test
    @DisplayName("processMonthlyBilling: cancela sem cobrar, gravar nem notificar quando o cliente está inativo")
    void processMonthlyBilling_cancelaSemCobrar_quandoClienteInativo() {
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
    @DisplayName("processMonthlyBilling: não cobra nem gera mensagem quando o cliente não existe (como no legado)")
    void processMonthlyBilling_naoFazNada_quandoClienteInexistente() {
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
    @DisplayName("processMonthlyBilling: aplica BLACKFRIDAY só em novembro quando usa a data do Clock")
    void processMonthlyBilling_aplicaBlackFridaySoEmNovembro_quandoUsaDataDoClock() {
        BillingCommand comando = comando(CLIENTE, "100", ADESAO_RECENTE, "BLACKFRIDAY");

        assertEquals(Money.of("70.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 11, 20))
                .processMonthlyBilling(comando).amount());
        assertEquals(Money.of("100.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 10, 31))
                .processMonthlyBilling(comando).amount());
    }

    @Test
    @DisplayName("processMonthlyBilling: conta os meses pela data do Clock quando calcula a fidelidade")
    void processMonthlyBilling_contaMesesPelaDataDoClock_quandoCalculaFidelidade() {
        BillingCommand comando = comando(CLIENTE, "100", LocalDate.of(2024, 9, 15), null);

        assertEquals(Money.of("90.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 9, 15))
                .processMonthlyBilling(comando).amount());
        assertEquals(Money.of("85.00"), servico(FakePaymentGateway.approving(), LocalDate.of(2026, 10, 15))
                .processMonthlyBilling(comando).amount());
    }

    @Test
    @DisplayName("processMonthlyBilling: usa o mesmo valor no gateway, na fatura e no recibo quando há desconto")
    void processMonthlyBilling_usaMesmoValorEmTodasAsPortas_quandoHaDesconto() {
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
    @DisplayName("processMonthlyBilling: propaga InfrastructureException sem enviar recibo quando a gravação da fatura falha")
    void processMonthlyBilling_propagaInfrastructureException_quandoGravacaoDaFaturaFalha() {
        InfrastructureException falha = new InfrastructureException("Banco fora do ar", new RuntimeException());
        SubscriptionBillingService servico = new SubscriptionBillingService(customers,
                fatura -> {
                    throw falha;
                },
                FakePaymentGateway.approving(), notifications, relogioEm(HOJE));

        InfrastructureException erro = assertThrows(InfrastructureException.class,
                () -> servico.processMonthlyBilling(comando(CLIENTE, "100", ADESAO_RECENTE, null)));

        assertSame(falha, erro);
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    @DisplayName("processMonthlyBilling: propaga InfrastructureException sem cobrar quando a busca do cliente falha")
    void processMonthlyBilling_propagaInfrastructureExceptionSemCobrar_quandoBuscaDoClienteFalha() {
        FakePaymentGateway payments = FakePaymentGateway.approving();
        SubscriptionBillingService servico = new SubscriptionBillingService(
                id -> {
                    throw new InfrastructureException("Banco fora do ar", new RuntimeException());
                },
                invoices, payments, notifications, relogioEm(HOJE));

        assertThrows(InfrastructureException.class,
                () -> servico.processMonthlyBilling(comando(CLIENTE, "100", ADESAO_RECENTE, null)));

        assertTrue(payments.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
    }
}
