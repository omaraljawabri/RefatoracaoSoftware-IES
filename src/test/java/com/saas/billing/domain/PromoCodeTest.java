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
    @DisplayName("from: reconhece o cupom quando o código está em qualquer caixa (como no legado)")
    void from_reconheceCupom_quandoCodigoEmQualquerCaixa(String texto, PromoCode esperado) {
        assertEquals(Optional.of(esperado), PromoCode.from(texto));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "NATAL", "WELCOME", "WELCOME100"})
    @DisplayName("from: retorna vazio quando o cupom é nulo, em branco ou desconhecido")
    void from_retornaVazio_quandoCupomNuloEmBrancoOuDesconhecido(String texto) {
        assertEquals(Optional.empty(), PromoCode.from(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {" BLACKFRIDAY", "BLACKFRIDAY ", " WELCOME10 "})
    @DisplayName("from: retorna vazio quando o código tem espaços (como no legado)")
    void from_retornaVazio_quandoCodigoTemEspacos(String texto) {
        assertEquals(Optional.empty(), PromoCode.from(texto));
    }
}
