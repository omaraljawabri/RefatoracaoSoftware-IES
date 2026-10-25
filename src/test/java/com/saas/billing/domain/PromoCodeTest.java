package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromoCodeTest {

    @ParameterizedTest
    @CsvSource({
            "BLACKFRIDAY, BLACKFRIDAY",
            "blackfriday, BLACKFRIDAY",
            "BlackFriday, BLACKFRIDAY",
            "WELCOME10, WELCOME10",
            "welcome10, WELCOME10"
    })
    @DisplayName("Reconhece o cupom sem diferenciar maiúsculas, como o código legado")
    void reconheceCupom(String texto, PromoCode esperado) {
        assertEquals(Optional.of(esperado), PromoCode.from(texto));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "NATAL", "WELCOME", "WELCOME100"})
    @DisplayName("Cupom nulo, em branco ou desconhecido não é reconhecido")
    void naoReconheceCupomInvalido(String texto) {
        assertEquals(Optional.empty(), PromoCode.from(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {" BLACKFRIDAY", "BLACKFRIDAY ", " WELCOME10 "})
    @DisplayName("Cupom com espaços não é reconhecido, como o código legado")
    void cupomComEspacosNaoEReconhecido(String texto) {
        assertEquals(Optional.empty(), PromoCode.from(texto));
    }
}
