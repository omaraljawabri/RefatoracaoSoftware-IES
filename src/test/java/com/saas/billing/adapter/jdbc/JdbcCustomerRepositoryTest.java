package com.saas.billing.adapter.jdbc;

import com.saas.billing.port.exception.InfrastructureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcCustomerRepositoryTest {

    @Test
    @DisplayName("findById: lança InfrastructureException quando o banco falha")
    void findById_lancaInfrastructureException_quandoBancoFalha() {
        JdbcCustomerRepository repositorio = new JdbcCustomerRepository(
                new JdbcConnectionFactory("jdbc:inexistente:teste", "u", "p"));

        assertThrows(InfrastructureException.class, () -> repositorio.findById("cus_123"));
    }
}
