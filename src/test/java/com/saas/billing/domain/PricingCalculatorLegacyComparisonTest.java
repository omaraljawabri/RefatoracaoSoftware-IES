package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de comparação com o legado: verifica se o {@link PricingCalculator} dá o
 * mesmo resultado da fórmula original do código legado (commit {@code 36d0970},
 * linhas 24–57), copiada sem alterações, em todas as combinações de entrada.
 *
 * O legado calcula em {@code double} e nunca arredonda; o novo cálculo devolve
 * 2 casas. Por isso o resultado novo deve estar a no máximo meio centavo do
 * valor do legado (mais uma folga para o erro do {@code double}).
 */
class PricingCalculatorLegacyComparisonTest {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.005000001");

    private static final double[] PRECOS = {0.01, 9.99, 10.0, 19.9, 29.99, 33.33, 49.9, 59.9, 99.9, 100.0,
            149.99, 333.33, 1234.56};
    private static final int[] MESES_DE_ASSINATURA = {-1, 0, 1, 2, 12, 13, 24, 25, 40};
    private static final boolean[] ONG = {false, true};
    private static final String[] CUPONS = {null, "", "BLACKFRIDAY", "blackfriday", "WELCOME10", "welcome10",
            " BLACKFRIDAY", "NATAL50"};
    private static final int[] DIAS_DE_ATRASO = {0, 1, 5, 17, 30};
    private static final LocalDate[] DATAS = {LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 20)};

    private final PricingCalculator calculator = new PricingCalculator();

    /** Cópia literal do cálculo do legado, com {@code LocalDate.now()} trocado pelo parâmetro {@code today}. */
    private static double legacyPrice(double basePrice, LocalDate signupDate, int overdueDays, String promoCode,
                                      boolean isNonProfit, LocalDate today) {
        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        double calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice - (basePrice * 0.15);
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice - (basePrice * 0.10);
        }

        if (isNonProfit) {
            calculatedPrice = calculatedPrice * 0.70;
        }

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice - 30.0;
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice * 0.90;
            }
        }

        if (overdueDays > 0) {
            double penalty = calculatedPrice * 0.02;
            double dailyInterest = (calculatedPrice * 0.001) * overdueDays;
            calculatedPrice = calculatedPrice + penalty + dailyInterest;
        }

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }
        return calculatedPrice;
    }

    @Test
    @DisplayName("calculate: fica a no máximo meio centavo do legado quando comparado em todas as combinações")
    void calculate_ficaAMeioCentavoDoLegado_quandoComparadoEmTodasAsCombinacoes() {
        List<String> divergencias = new ArrayList<>();
        int casos = 0;

        for (double preco : PRECOS) {
            for (int meses : MESES_DE_ASSINATURA) {
                for (boolean ong : ONG) {
                    for (String cupom : CUPONS) {
                        for (int dias : DIAS_DE_ATRASO) {
                            for (LocalDate hoje : DATAS) {
                                casos++;
                                LocalDate adesao = hoje.minusMonths(meses);
                                BigDecimal legado = BigDecimal.valueOf(legacyPrice(preco, adesao, dias, cupom, ong, hoje));
                                BigDecimal novo = calculator.calculate(
                                        new BillingCommand("cus_1", Money.of(preco), adesao, dias, cupom, ong, "tok_x"),
                                        hoje).amount();

                                if (novo.subtract(legado).abs().compareTo(TOLERANCIA) > 0) {
                                    divergencias.add(String.format("preço=%s meses=%d ong=%s cupom=%s atraso=%d hoje=%s:"
                                            + " legado=%s novo=%s", preco, meses, ong, cupom, dias, hoje, legado, novo));
                                }
                            }
                        }
                    }
                }
            }
        }

        int total = casos;
        assertEquals(PRECOS.length * MESES_DE_ASSINATURA.length * ONG.length * CUPONS.length
                * DIAS_DE_ATRASO.length * DATAS.length, total);
        assertTrue(divergencias.isEmpty(), () -> divergencias.size() + " de " + total + " casos divergem:\n"
                + String.join("\n", divergencias.subList(0, Math.min(10, divergencias.size()))));
    }
}
