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
    @DisplayName("from: converte o preço para Money quando recebe um double")
    void from_convertePrecoParaMoney_quandoRecebeDouble() {
        assertEquals(Money.of("99.90"), comandoPadrao().basePrice());
    }

    @Test
    @DisplayName("from: converte a data de adesão quando recebe texto no formato ISO")
    void from_converteDataDeAdesao_quandoRecebeTextoIso() {
        assertEquals(LocalDate.of(2024, 1, 31), comandoPadrao().signupDate());
    }

    @Test
    @DisplayName("from: mantém os demais dados quando recebe valores válidos")
    void from_mantemDemaisDados_quandoRecebeValoresValidos() {
        BillingCommand comando = comandoPadrao();

        assertEquals("cus_123", comando.customerId());
        assertEquals(5, comando.overdueDays());
        assertEquals("WELCOME10", comando.promoCode());
        assertTrue(comando.nonProfit());
        assertEquals("tok_abc", comando.paymentCardToken());
    }

    @Test
    @DisplayName("from: aceita cupom e token nulos quando não são informados (como no legado)")
    void from_aceitaCupomETokenNulos_quandoNaoInformados() {
        BillingCommand comando = BillingCommand.from("cus_123", 50.0, "2024-01-31", 0, null, false, null);

        assertNull(comando.promoCode());
        assertNull(comando.paymentCardToken());
        assertFalse(comando.nonProfit());
    }

    @Test
    @DisplayName("from: lança DateTimeParseException quando a data é inválida (como no legado)")
    void from_lancaDateTimeParseException_quandoDataInvalida() {
        assertThrows(DateTimeParseException.class,
                () -> BillingCommand.from("cus_123", 50.0, "31/01/2024", 0, null, false, null));
    }

    @Test
    @DisplayName("from: lança NullPointerException quando a data é nula (como no legado)")
    void from_lancaNullPointerException_quandoDataNula() {
        assertThrows(NullPointerException.class,
                () -> BillingCommand.from("cus_123", 50.0, null, 0, null, false, null));
    }

    @Test
    @DisplayName("constructor: lança NullPointerException quando o preço ou a data são nulos")
    void constructor_lancaNullPointerException_quandoPrecoOuDataNulos() {
        assertThrows(NullPointerException.class,
                () -> new BillingCommand("cus_123", null, LocalDate.of(2024, 1, 31), 0, null, false, null));
        assertThrows(NullPointerException.class,
                () -> new BillingCommand("cus_123", Money.of(50), null, 0, null, false, null));
    }

    @Test
    @DisplayName("equals: retorna true e o mesmo hashCode quando os dados são iguais")
    void equals_retornaTrue_quandoMesmosDados() {
        assertEquals(comandoPadrao(), comandoPadrao());
        assertEquals(comandoPadrao().hashCode(), comandoPadrao().hashCode());
    }
}