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
    @DisplayName("save: lança InfrastructureException quando o banco falha")
    void save_lancaInfrastructureException_quandoBancoFalha() {
        JdbcInvoiceRepository repositorio = new JdbcInvoiceRepository(
                new JdbcConnectionFactory("jdbc:inexistente:teste", "u", "p"));
        Invoice fatura = new Invoice("cus_123", Money.of("59.50"), InvoiceStatus.PAID);

        assertThrows(InfrastructureException.class, () -> repositorio.save(fatura));
    }
}
