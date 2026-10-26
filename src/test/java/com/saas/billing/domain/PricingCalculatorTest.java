package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingCalculatorTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);
    private static final LocalDate NOVEMBRO = LocalDate.of(2026, 11, 20);
    private static final LocalDate ADESAO_RECENTE = LocalDate.of(2026, 5, 15); // 5 meses antes de HOJE

    private final PricingCalculator calculator = new PricingCalculator();

    private static BillingCommand comando(String preco, LocalDate adesao, int diasAtraso,
                                          String cupom, boolean ong) {
        return new BillingCommand("cus_1", Money.of(preco), adesao, diasAtraso, cupom, ong, "tok_x");
    }

    private Money calcular(String preco, LocalDate adesao, int diasAtraso, String cupom,
                           boolean ong, LocalDate hoje) {
        return calculator.calculate(comando(preco, adesao, diasAtraso, cupom, ong), hoje);
    }

    // ---------- Fidelidade ----------

    @Test
    @DisplayName("Sem fidelidade, cupom, ONG ou atraso, cobra o preço base")
    void semDescontos() {
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, null, false, HOJE));
    }

    @Test
    @DisplayName("Com exatamente 12 meses ainda não há desconto de fidelidade")
    void dozeMesesSemDesconto() {
        assertEquals(Money.of("100.00"), calcular("100", LocalDate.of(2025, 10, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("Com 13 meses, 10% de desconto")
    void trezeMesesDezPorCento() {
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2025, 9, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("Com exatamente 24 meses, ainda 10% de desconto")
    void vinteEQuatroMesesDezPorCento() {
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2024, 10, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("Com 25 meses, 15% de desconto")
    void vinteECincoMesesQuinzePorCento() {
        assertEquals(Money.of("85.00"), calcular("100", LocalDate.of(2024, 9, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("Meses ativos ignoram o dia do mês, como o legado (31/01 a 01/02 = 1 mês)")
    void mesesAtivosIgnoramODia() {
        assertEquals(1, PricingCalculator.monthsActive(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1)));
    }

    // ---------- ONG ----------

    @Test
    @DisplayName("ONG tem 30% de desconto")
    void ongTrintaPorCento() {
        assertEquals(Money.of("70.00"), calcular("100", ADESAO_RECENTE, 0, null, true, HOJE));
    }

    @Test
    @DisplayName("Fidelidade de 15% + ONG sobre R$ 100,00 resulta em R$ 59,50")
    void fidelidadeMaisOng() {
        assertEquals(Money.of("59.50"), calcular("100", LocalDate.of(2024, 9, 15), 0, null, true, HOJE));
    }

    // ---------- Cupons ----------

    @Test
    @DisplayName("BLACKFRIDAY em novembro tira R$ 30,00")
    void blackFridayEmNovembro() {
        assertEquals(Money.of("70.00"), calcular("100", ADESAO_RECENTE, 0, "BLACKFRIDAY", false, NOVEMBRO));
    }

    @Test
    @DisplayName("BLACKFRIDAY fora de novembro não tem efeito")
    void blackFridayForaDeNovembro() {
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, "BLACKFRIDAY", false, HOJE));
    }

    @Test
    @DisplayName("Cupom é aceito em minúsculas")
    void cupomEmMinusculas() {
        assertEquals(Money.of("70.00"), calcular("100", ADESAO_RECENTE, 0, "blackfriday", false, NOVEMBRO));
    }

    @Test
    @DisplayName("WELCOME10 com até 1 mês de assinatura dá 10% de desconto")
    void welcomeComUmMes() {
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2026, 9, 15), 0, "WELCOME10", false, HOJE));
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2026, 10, 1), 0, "WELCOME10", false, HOJE));
    }

    @Test
    @DisplayName("WELCOME10 com 2 meses de assinatura não tem efeito")
    void welcomeComDoisMeses() {
        assertEquals(Money.of("100.00"), calcular("100", LocalDate.of(2026, 8, 15), 0, "WELCOME10", false, HOJE));
    }

    @Test
    @DisplayName("Cupom desconhecido, vazio ou nulo é ignorado")
    void cupomInvalidoIgnorado() {
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, "NATAL50", false, HOJE));
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, "  ", false, HOJE));
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, null, false, HOJE));
    }

    // ---------- Ordem dos descontos ----------

    @Test
    @DisplayName("Desconto de ONG vem antes do cupom: (100 × 0,70) − 30 = 40")
    void ongAntesDoCupom() {
        assertEquals(Money.of("40.00"), calcular("100", ADESAO_RECENTE, 0, "BLACKFRIDAY", true, NOVEMBRO));
    }

    // ---------- Atraso ----------

    @Test
    @DisplayName("Atraso de 10 dias: multa de 2% + juros de 0,1% ao dia")
    void multaEJuros() {
        assertEquals(Money.of("103.00"), calcular("100", ADESAO_RECENTE, 10, null, false, HOJE));
    }

    @Test
    @DisplayName("Multa e juros incidem sobre o valor já descontado")
    void multaSobreValorDescontado() {
        // 70,00 + 1,40 (multa) + 0,70 (juros)
        assertEquals(Money.of("72.10"), calcular("100", ADESAO_RECENTE, 10, null, true, HOJE));
    }

    // ---------- Piso zero ----------

    @Test
    @DisplayName("Valor negativo vira zero")
    void pisoZero() {
        assertEquals(Money.ZERO, calcular("20", ADESAO_RECENTE, 0, "BLACKFRIDAY", false, NOVEMBRO));
    }

    @Test
    @DisplayName("Piso zero é aplicado só depois da multa")
    void pisoZeroDepoisDaMulta() {
        assertEquals(Money.ZERO, calcular("20", ADESAO_RECENTE, 10, "BLACKFRIDAY", false, NOVEMBRO));
    }

    // ---------- Arredondamento ----------

    @Test
    @DisplayName("Arredonda só no final, como o legado: 10 × 0,90 × 0,70 + 2% + 0,1% = 6,4323 → R$ 6,43")
    void arredondaSoNoFinal() {
        // Arredondando a cada etapa, a multa (0,126) e os juros (0,0063) virariam
        // 0,13 e 0,01, e o total seria R$ 6,44.
        assertEquals(Money.of("6.43"), calcular("10", LocalDate.of(2025, 9, 15), 1, null, true, HOJE));
    }
}
