package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingCommandTest {

    private static BillingCommand comandoPadrao() {
        return BillingCommand.from("cus_123", 99.90, "2024-01-31", 5, "WELCOME10", true, "tok_abc");
    }

    @Test
    @DisplayName("Converte o preço base para Money")
    void converteOPrecoParaMoney() {
        assertEquals(Money.of("99.90"), comandoPadrao().basePrice());
    }

    @Test
    @DisplayName("Converte a data de adesão de texto para LocalDate")
    void converteADataDeAdesao() {
        assertEquals(LocalDate.of(2024, 1, 31), comandoPadrao().signupDate());
    }

    @Test
    @DisplayName("Mantém os demais dados como recebidos")
    void mantemOsDemaisDados() {
        BillingCommand comando = comandoPadrao();

        assertEquals("cus_123", comando.customerId());
        assertEquals(5, comando.overdueDays());
        assertEquals("WELCOME10", comando.promoCode());
        assertTrue(comando.nonProfit());
        assertEquals("tok_abc", comando.paymentCardToken());
    }

    @Test
    @DisplayName("Aceita cupom e token nulos, como o código legado")
    void aceitaCupomETokenNulos() {
        BillingCommand comando = BillingCommand.from("cus_123", 50.0, "2024-01-31", 0, null, false, null);

        assertNull(comando.promoCode());
        assertNull(comando.paymentCardToken());
        assertFalse(comando.nonProfit());
    }

    @Test
    @DisplayName("Data inválida lança DateTimeParseException, como o código legado")
    void dataInvalidaMantemOErroAtual() {
        assertThrows(DateTimeParseException.class,
                () -> BillingCommand.from("cus_123", 50.0, "31/01/2024", 0, null, false, null));
    }

    @Test
    @DisplayName("Data nula lança NullPointerException, como o código legado")
    void dataNulaMantemOErroAtual() {
        assertThrows(NullPointerException.class,
                () -> BillingCommand.from("cus_123", 50.0, null, 0, null, false, null));
    }

    @Test
    @DisplayName("Construtor direto não aceita preço nem data nulos")
    void construtorRejeitaPrecoEDataNulos() {
        assertThrows(NullPointerException.class,
                () -> new BillingCommand("cus_123", null, LocalDate.of(2024, 1, 31), 0, null, false, null));
        assertThrows(NullPointerException.class,
                () -> new BillingCommand("cus_123", Money.of(50), null, 0, null, false, null));
    }

    @Test
    @DisplayName("Comandos com os mesmos dados são iguais")
    void comandosComMesmosDadosSaoIguais() {
        assertEquals(comandoPadrao(), comandoPadrao());
        assertEquals(comandoPadrao().hashCode(), comandoPadrao().hashCode());
    }
}