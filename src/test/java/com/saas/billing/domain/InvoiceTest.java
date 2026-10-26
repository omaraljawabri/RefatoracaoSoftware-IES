package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceTest {

    @Test
    @DisplayName("constructor: guarda cliente, valor e status quando os dados são válidos")
    void constructor_guardaClienteValorEStatus_quandoDadosValidos() {
        Invoice fatura = new Invoice("cus_123", Money.of("59.50"), InvoiceStatus.PAID);

        assertEquals("cus_123", fatura.customerId());
        assertEquals(Money.of("59.50"), fatura.amount());
        assertEquals(InvoiceStatus.PAID, fatura.status());
    }

    @Test
    @DisplayName("constructor: lança NullPointerException quando algum campo é nulo")
    void constructor_lancaNullPointerException_quandoAlgumCampoNulo() {
        assertThrows(NullPointerException.class, () -> new Invoice(null, Money.of(10), InvoiceStatus.PAID));
        assertThrows(NullPointerException.class, () -> new Invoice("cus_123", null, InvoiceStatus.PAID));
        assertThrows(NullPointerException.class, () -> new Invoice("cus_123", Money.of(10), null));
    }
}
