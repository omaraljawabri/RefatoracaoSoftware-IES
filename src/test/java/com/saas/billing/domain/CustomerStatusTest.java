package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerStatusTest {

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "active", "Active"})
    @DisplayName("ACTIVE é reconhecido sem diferenciar maiúsculas, como o código legado")
    void reconheceAtivo(String valor) {
        assertEquals(CustomerStatus.ACTIVE, CustomerStatus.from(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INACTIVE", "SUSPENDED", "CANCELLED", " ACTIVE", "ACTIVE "})
    @DisplayName("Qualquer outro valor, inclusive nulo ou com espaços, é inativo, como o código legado")
    void demaisValoresSaoInativos(String valor) {
        assertEquals(CustomerStatus.INACTIVE, CustomerStatus.from(valor));
    }
}
