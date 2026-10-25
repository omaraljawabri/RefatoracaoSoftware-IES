package com.saas.billing.adapter.jdbc;

import com.saas.billing.port.exception.InfrastructureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcCustomerRepositoryTest {

    @Test
    @DisplayName("Falha do banco chega a quem chamou como InfrastructureException, sem ser silenciada")
    void falhaDoBancoNaoESilenciada() {
        JdbcCustomerRepository repositorio = new JdbcCustomerRepository(
                new JdbcConnectionFactory("jdbc:inexistente:teste", "u", "p"));

        assertThrows(InfrastructureException.class, () -> repositorio.findById("cus_123"));
    }
}
