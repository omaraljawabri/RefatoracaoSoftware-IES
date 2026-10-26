package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerTest {

    @Test
    @DisplayName("isActive: retorna true quando o status é ACTIVE")
    void isActive_retornaTrue_quandoStatusActive() {
        assertTrue(new Customer("cus_123", "a@b.com", CustomerStatus.ACTIVE).isActive());
    }

    @Test
    @DisplayName("isActive: retorna false quando o status é INACTIVE")
    void isActive_retornaFalse_quandoStatusInactive() {
        assertFalse(new Customer("cus_123", "a@b.com", CustomerStatus.INACTIVE).isActive());
    }

    @Test
    @DisplayName("constructor: lança NullPointerException quando o id ou o status são nulos")
    void constructor_lancaNullPointerException_quandoIdOuStatusNulos() {
        assertThrows(NullPointerException.class, () -> new Customer(null, "a@b.com", CustomerStatus.ACTIVE));
        assertThrows(NullPointerException.class, () -> new Customer("cus_123", "a@b.com", null));
    }
}
