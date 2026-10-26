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
    @DisplayName("calculate: cobra o preço base quando não há desconto nem atraso")
    void calculate_cobraPrecoBase_quandoSemDescontosNemAtraso() {
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, null, false, HOJE));
    }

    @Test
    @DisplayName("calculate: não dá desconto de fidelidade quando a assinatura tem exatamente 12 meses")
    void calculate_naoDaDescontoDeFidelidade_quandoAssinaturaTemDozeMeses() {
        assertEquals(Money.of("100.00"), calcular("100", LocalDate.of(2025, 10, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("calculate: dá 10% de desconto quando a assinatura tem 13 meses")
    void calculate_daDezPorCentoDeDesconto_quandoAssinaturaTemTrezeMeses() {
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2025, 9, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("calculate: dá 10% de desconto quando a assinatura tem exatamente 24 meses")
    void calculate_daDezPorCentoDeDesconto_quandoAssinaturaTemVinteEQuatroMeses() {
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2024, 10, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("calculate: dá 15% de desconto quando a assinatura tem 25 meses")
    void calculate_daQuinzePorCentoDeDesconto_quandoAssinaturaTemVinteECincoMeses() {
        assertEquals(Money.of("85.00"), calcular("100", LocalDate.of(2024, 9, 15), 0, null, false, HOJE));
    }

    @Test
    @DisplayName("monthsActive: conta 1 mês quando vai de 31/01 a 01/02, ignorando o dia (como no legado)")
    void monthsActive_contaUmMes_quandoVaiDe31DeJaneiroA1DeFevereiro() {
        assertEquals(1, PricingCalculator.monthsActive(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1)));
    }

    // ---------- ONG ----------

    @Test
    @DisplayName("calculate: dá 30% de desconto quando o cliente é ONG")
    void calculate_daTrintaPorCentoDeDesconto_quandoClienteEOng() {
        assertEquals(Money.of("70.00"), calcular("100", ADESAO_RECENTE, 0, null, true, HOJE));
    }

    @Test
    @DisplayName("calculate: aplica o desconto de ONG sobre o preço com fidelidade quando a ONG tem 25 meses (R$ 59,50)")
    void calculate_aplicaOngSobrePrecoComFidelidade_quandoOngTemVinteECincoMeses() {
        assertEquals(Money.of("59.50"), calcular("100", LocalDate.of(2024, 9, 15), 0, null, true, HOJE));
    }

    // ---------- Cupons ----------

    @Test
    @DisplayName("calculate: desconta R$ 30,00 quando o cupom BLACKFRIDAY é usado em novembro")
    void calculate_descontaTrintaReais_quandoBlackFridayEmNovembro() {
        assertEquals(Money.of("70.00"), calcular("100", ADESAO_RECENTE, 0, "BLACKFRIDAY", false, NOVEMBRO));
    }

    @Test
    @DisplayName("calculate: ignora o cupom quando BLACKFRIDAY é usado fora de novembro")
    void calculate_ignoraCupom_quandoBlackFridayForaDeNovembro() {
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, "BLACKFRIDAY", false, HOJE));
    }

    @Test
    @DisplayName("calculate: aplica o cupom quando o código está em minúsculas")
    void calculate_aplicaCupom_quandoCodigoEmMinusculas() {
        assertEquals(Money.of("70.00"), calcular("100", ADESAO_RECENTE, 0, "blackfriday", false, NOVEMBRO));
    }

    @Test
    @DisplayName("calculate: dá 10% de desconto quando WELCOME10 é usado com até 1 mês de assinatura")
    void calculate_daDezPorCentoDeDesconto_quandoWelcome10ComAteUmMes() {
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2026, 9, 15), 0, "WELCOME10", false, HOJE));
        assertEquals(Money.of("90.00"), calcular("100", LocalDate.of(2026, 10, 1), 0, "WELCOME10", false, HOJE));
    }

    @Test
    @DisplayName("calculate: ignora o cupom quando WELCOME10 é usado com 2 meses de assinatura")
    void calculate_ignoraCupom_quandoWelcome10ComDoisMeses() {
        assertEquals(Money.of("100.00"), calcular("100", LocalDate.of(2026, 8, 15), 0, "WELCOME10", false, HOJE));
    }

    @Test
    @DisplayName("calculate: ignora o cupom quando ele é desconhecido, vazio ou nulo")
    void calculate_ignoraCupom_quandoCupomDesconhecidoVazioOuNulo() {
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, "NATAL50", false, HOJE));
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, "  ", false, HOJE));
        assertEquals(Money.of("100.00"), calcular("100", ADESAO_RECENTE, 0, null, false, HOJE));
    }

    // ---------- Ordem dos descontos ----------

    @Test
    @DisplayName("calculate: aplica o desconto de ONG antes do cupom quando a ONG usa BLACKFRIDAY ((100 × 0,70) − 30 = 40)")
    void calculate_aplicaOngAntesDoCupom_quandoOngUsaBlackFriday() {
        assertEquals(Money.of("40.00"), calcular("100", ADESAO_RECENTE, 0, "BLACKFRIDAY", true, NOVEMBRO));
    }

    // ---------- Atraso ----------

    @Test
    @DisplayName("calculate: cobra multa de 2% e juros de 0,1% ao dia quando o pagamento está atrasado")
    void calculate_cobraMultaEJuros_quandoPagamentoAtrasado() {
        assertEquals(Money.of("103.00"), calcular("100", ADESAO_RECENTE, 10, null, false, HOJE));
    }

    @Test
    @DisplayName("calculate: cobra multa e juros sobre o valor já descontado quando a ONG paga com atraso")
    void calculate_cobraMultaSobreValorDescontado_quandoOngPagaComAtraso() {
        // 70,00 + 1,40 (multa) + 0,70 (juros)
        assertEquals(Money.of("72.10"), calcular("100", ADESAO_RECENTE, 10, null, true, HOJE));
    }

    // ---------- Piso zero ----------

    @Test
    @DisplayName("calculate: retorna zero quando o desconto supera o preço")
    void calculate_retornaZero_quandoDescontoSuperaPreco() {
        assertEquals(Money.ZERO, calcular("20", ADESAO_RECENTE, 0, "BLACKFRIDAY", false, NOVEMBRO));
    }

    @Test
    @DisplayName("calculate: aplica o piso zero depois da multa quando o desconto supera o preço com atraso")
    void calculate_aplicaPisoZeroDepoisDaMulta_quandoDescontoSuperaPrecoComAtraso() {
        assertEquals(Money.ZERO, calcular("20", ADESAO_RECENTE, 10, "BLACKFRIDAY", false, NOVEMBRO));
    }

    // ---------- Arredondamento ----------

    @Test
    @DisplayName("calculate: arredonda só no final quando multa e juros são fracionados (6,4323 → R$ 6,43, como no legado)")
    void calculate_arredondaSoNoFinal_quandoMultaEJurosSaoFracionados() {
        // Arredondando a cada etapa, a multa (0,126) e os juros (0,0063) virariam
        // 0,13 e 0,01, e o total seria R$ 6,44.
        assertEquals(Money.of("6.43"), calcular("10", LocalDate.of(2025, 9, 15), 1, null, true, HOJE));
    }
}
