package com.saas.billing.adapter.jdbc;

import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.port.exception.InfrastructureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcInvoiceRepositoryTest {

    @Test
    @DisplayName("Falha do banco chega a quem chamou como InfrastructureException, sem ser silenciada")
    void falhaDoBancoNaoESilenciada() {
        JdbcInvoiceRepository repositorio = new JdbcInvoiceRepository(
                new JdbcConnectionFactory("jdbc:inexistente:teste", "u", "p"));
        Invoice fatura = new Invoice("cus_123", Money.of("59.50"), InvoiceStatus.PAID);

        assertThrows(InfrastructureException.class, () -> repositorio.save(fatura));
    }
}
