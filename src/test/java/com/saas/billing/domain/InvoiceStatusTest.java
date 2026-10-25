package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceStatusTest {

    @Test
    @DisplayName("Pagamento aprovado gera fatura PAID")
    void pagamentoAprovadoGeraPaid() {
        assertEquals(InvoiceStatus.PAID, InvoiceStatus.of(true));
    }

    @Test
    @DisplayName("Pagamento recusado gera fatura FAILED")
    void pagamentoRecusadoGeraFailed() {
        assertEquals(InvoiceStatus.FAILED, InvoiceStatus.of(false));
    }

    @Test
    @DisplayName("Os nomes são os mesmos textos gravados pelo código legado")
    void nomesIguaisAosDoLegado() {
        assertEquals("PAID", InvoiceStatus.PAID.name());
        assertEquals("FAILED", InvoiceStatus.FAILED.name());
    }
}
