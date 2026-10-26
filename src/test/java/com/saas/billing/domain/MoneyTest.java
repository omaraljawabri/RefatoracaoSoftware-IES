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
    @DisplayName("of: guarda 2 casas decimais quando recebe um inteiro")
    void of_guardaDuasCasasDecimais_quandoRecebeInteiro() {
        assertEquals(new BigDecimal("10.00"), Money.of(10).amount());
    }

    @Test
    @DisplayName("equals: retorna true quando o valor é o mesmo com escalas diferentes")
    void equals_retornaTrue_quandoMesmoValorComEscalasDiferentes() {
        assertEquals(Money.of(10), Money.of("10.00"));
        assertEquals(Money.of(10).hashCode(), Money.of("10.00").hashCode());
    }

    @Test
    @DisplayName("of: arredonda para 2 casas quando o double tem erro de ponto flutuante")
    void of_arredondaParaDuasCasas_quandoDoubleTemErroDePontoFlutuante() {
        assertEquals(Money.of("59.50"), Money.of(59.49999999999999));
    }

    @Test
    @DisplayName("times: calcula sem erro de ponto flutuante quando aplica fatores em sequência (100 × 0,85 × 0,70 = 59,50)")
    void times_calculaSemErroDePontoFlutuante_quandoAplicaFatoresEmSequencia() {
        Money resultado = Money.of(100)
                .times(new BigDecimal("0.85"))
                .times(new BigDecimal("0.70"));

        assertEquals(Money.of("59.50"), resultado);
    }

    @Test
    @DisplayName("of: arredonda para o par mais próximo (HALF_EVEN) quando o valor tem meio centavo")
    void of_arredondaParaOParMaisProximo_quandoValorTemMeioCentavo() {
        assertEquals(Money.of("1.78"), Money.of("1.785"));
        assertEquals(Money.of("1.78"), Money.of("1.775"));
        assertEquals(Money.of("1.79"), Money.of("1.786"));
    }

    @Test
    @DisplayName("plus: devolve a soma quando recebe outro Money")
    void plus_devolveSoma_quandoRecebeOutroMoney() {
        assertEquals(Money.of("10.30"), Money.of("10.10").plus(Money.of("0.20")));
    }

    @Test
    @DisplayName("minus: devolve a diferença quando recebe outro Money")
    void minus_devolveDiferenca_quandoRecebeOutroMoney() {
        assertEquals(Money.of("70.00"), Money.of("100.00").minus(Money.of("30.00")));
    }

    @Test
    @DisplayName("plus: não altera o objeto original quando é chamado")
    void plus_naoAlteraOriginal_quandoChamado() {
        Money original = Money.of(100);

        original.plus(Money.of(30));

        assertEquals(Money.of(100), original);
    }

    @Test
    @DisplayName("minus: não altera o objeto original quando é chamado")
    void minus_naoAlteraOriginal_quandoChamado() {
        Money original = Money.of(100);

        original.minus(Money.of(30));

        assertEquals(Money.of(100), original);
    }

    @Test
    @DisplayName("times: não altera o objeto original quando é chamado")
    void times_naoAlteraOriginal_quandoChamado() {
        Money original = Money.of(100);

        original.times(new BigDecimal("0.70"));

        assertEquals(Money.of(100), original);
    }

    @Test
    @DisplayName("isNegative: retorna true quando o valor é menor que zero")
    void isNegative_retornaTrue_quandoValorMenorQueZero() {
        assertTrue(Money.of(20).minus(Money.of(50)).isNegative());
    }

    @Test
    @DisplayName("isNegative: retorna false quando o valor é positivo")
    void isNegative_retornaFalse_quandoValorPositivo() {
        assertFalse(Money.of(20).isNegative());
    }

    @Test
    @DisplayName("atLeastZero: retorna zero quando o valor é negativo")
    void atLeastZero_retornaZero_quandoValorNegativo() {
        assertEquals(Money.ZERO, Money.of(20).minus(Money.of(50)).atLeastZero());
    }

    @Test
    @DisplayName("atLeastZero: retorna o próprio objeto quando o valor é positivo")
    void atLeastZero_retornaProprioObjeto_quandoValorPositivo() {
        Money positivo = Money.of(20);

        assertSame(positivo, positivo.atLeastZero());
    }

    @Test
    @DisplayName("formatted: usa o padrão brasileiro (R$ 1.234,56) quando formata o valor")
    void formatted_usaPadraoBrasileiro_quandoFormataValor() {
        assertEquals("R$ 59,50", Money.of("59.5").formatted());
        assertEquals("R$ 1.234,56", Money.of("1234.56").formatted());
        assertEquals("R$ 0,00", Money.ZERO.formatted());
    }

    @Test
    @DisplayName("constructor: lança NullPointerException quando o valor é nulo")
    void constructor_lancaNullPointerException_quandoValorNulo() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }

    @Test
    @DisplayName("times: lança NullPointerException quando o fator é nulo")
    void times_lancaNullPointerException_quandoFatorNulo() {
        assertThrows(NullPointerException.class, () -> Money.of(10).times(null));
    }
}