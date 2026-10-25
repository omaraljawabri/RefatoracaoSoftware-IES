package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    @DisplayName("Guarda o valor sempre com 2 casas decimais")
    void guardaComDuasCasas() {
        assertEquals(new BigDecimal("10.00"), Money.of(10).amount());
    }

    @Test
    @DisplayName("Valores iguais com escalas diferentes são iguais")
    void igualdadeIgnoraEscala() {
        assertEquals(Money.of(10), Money.of("10.00"));
        assertEquals(Money.of(10).hashCode(), Money.of("10.00").hashCode());
    }

    @Test
    @DisplayName("Corrige o valor quebrado gerado pelo double no código legado")
    void corrigeValorQuebradoDoDouble() {
        assertEquals(Money.of("59.50"), Money.of(59.49999999999999));
    }

    @Test
    @DisplayName("Fidelidade de 15% + desconto de ONG sobre R$ 100,00 resulta em R$ 59,50")
    void calculaDescontosSemErroDePontoFlutuante() {
        Money resultado = Money.of(100)
                .times(new BigDecimal("0.85"))
                .times(new BigDecimal("0.70"));

        assertEquals(Money.of("59.50"), resultado);
    }

    @Test
    @DisplayName("Arredonda meio centavo para o par mais próximo (HALF_EVEN)")
    void usaArredondamentoBancario() {
        assertEquals(Money.of("1.78"), Money.of("1.785"));
        assertEquals(Money.of("1.78"), Money.of("1.775"));
        assertEquals(Money.of("1.79"), Money.of("1.786"));
    }

    @Test
    @DisplayName("Soma e subtrai valores")
    void somaESubtrai() {
        assertEquals(Money.of("10.30"), Money.of("10.10").plus(Money.of("0.20")));
        assertEquals(Money.of("70.00"), Money.of("100.00").minus(Money.of("30.00")));
    }

    @Test
    @DisplayName("Operações não alteram o objeto original")
    void eImutavel() {
        Money original = Money.of(100);

        original.minus(Money.of(30));
        original.plus(Money.of(30));
        original.times(new BigDecimal("0.70"));

        assertEquals(Money.of(100), original);
    }

    @Test
    @DisplayName("Valor negativo vira zero; valor positivo continua o mesmo")
    void limitaAZero() {
        Money negativo = Money.of(20).minus(Money.of(50));
        Money positivo = Money.of(20);

        assertTrue(negativo.isNegative());
        assertEquals(Money.ZERO, negativo.atLeastZero());
        assertFalse(positivo.isNegative());
        assertSame(positivo, positivo.atLeastZero());
    }

    @Test
    @DisplayName("Formata no padrão brasileiro")
    void formataEmReais() {
        assertEquals("R$ 59,50", Money.of("59.5").formatted());
        assertEquals("R$ 1.234,56", Money.of("1234.56").formatted());
        assertEquals("R$ 0,00", Money.ZERO.formatted());
    }

    @Test
    @DisplayName("Não aceita valor nem fator nulos")
    void rejeitaNulos() {
        assertThrows(NullPointerException.class, () -> new Money(null));
        assertThrows(NullPointerException.class, () -> Money.of(10).times(null));
    }
}