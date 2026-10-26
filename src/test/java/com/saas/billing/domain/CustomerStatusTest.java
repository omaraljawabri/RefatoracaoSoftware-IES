package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerStatusTest {

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "active", "Active"})
    @DisplayName("from: retorna ACTIVE quando o texto é ACTIVE em qualquer caixa (como no legado)")
    void from_retornaActive_quandoTextoEActiveEmQualquerCaixa(String valor) {
        assertEquals(CustomerStatus.ACTIVE, CustomerStatus.from(valor));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INACTIVE", "SUSPENDED", "CANCELLED", " ACTIVE", "ACTIVE "})
    @DisplayName("from: retorna INACTIVE quando o texto é diferente de ACTIVE, nulo ou com espaços (como no legado)")
    void from_retornaInactive_quandoTextoDiferenteDeActive(String valor) {
        assertEquals(CustomerStatus.INACTIVE, CustomerStatus.from(valor));
    }
}
