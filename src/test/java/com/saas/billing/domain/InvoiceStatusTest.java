package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceStatusTest {

    @Test
    @DisplayName("of: retorna PAID quando o pagamento é aprovado")
    void of_retornaPaid_quandoPagamentoAprovado() {
        assertEquals(InvoiceStatus.PAID, InvoiceStatus.of(true));
    }

    @Test
    @DisplayName("of: retorna FAILED quando o pagamento é recusado")
    void of_retornaFailed_quandoPagamentoRecusado() {
        assertEquals(InvoiceStatus.FAILED, InvoiceStatus.of(false));
    }

    @Test
    @DisplayName("name: retorna o mesmo texto do legado quando o status é gravado no banco")
    void name_retornaTextoDoLegado_quandoGravadoNoBanco() {
        assertEquals("PAID", InvoiceStatus.PAID.name());
        assertEquals("FAILED", InvoiceStatus.FAILED.name());
    }
}
