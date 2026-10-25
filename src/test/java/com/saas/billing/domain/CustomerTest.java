package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerTest {

    @Test
    @DisplayName("Cliente com status ACTIVE está ativo")
    void clienteAtivo() {
        assertTrue(new Customer("cus_123", "a@b.com", CustomerStatus.ACTIVE).isActive());
    }

    @Test
    @DisplayName("Cliente com status INACTIVE não está ativo")
    void clienteInativo() {
        assertFalse(new Customer("cus_123", "a@b.com", CustomerStatus.INACTIVE).isActive());
    }

    @Test
    @DisplayName("Não aceita id nem status nulos")
    void rejeitaNulos() {
        assertThrows(NullPointerException.class, () -> new Customer(null, "a@b.com", CustomerStatus.ACTIVE));
        assertThrows(NullPointerException.class, () -> new Customer("cus_123", "a@b.com", null));
    }
}
