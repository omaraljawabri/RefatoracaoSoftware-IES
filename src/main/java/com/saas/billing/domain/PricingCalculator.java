package com.saas.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;

/**
 * Regras de preço da cobrança mensal, sem acesso a banco, rede ou relógio.
 *
 * <p>Reproduz o cálculo do código legado (linhas 25–57), na mesma ordem:</p>
 * <ol>
 *   <li>desconto de fidelidade, calculado sobre o preço base;</li>
 *   <li>desconto de ONG;</li>
 *   <li>cupom promocional (no máximo um);</li>
 *   <li>multa e juros por atraso;</li>
 *   <li>piso zero, aplicado só no final.</li>
 * </ol>
 */
public class PricingCalculator {

    private static final int LONG_LOYALTY_MONTHS = 24;
    private static final int SHORT_LOYALTY_MONTHS = 12;
    private static final BigDecimal LONG_LOYALTY_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal SHORT_LOYALTY_DISCOUNT = new BigDecimal("0.10");

    private static final BigDecimal NON_PROFIT_FACTOR = new BigDecimal("0.70");

    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");
    private static final Month BLACK_FRIDAY_MONTH = Month.NOVEMBER;
    private static final BigDecimal WELCOME_FACTOR = new BigDecimal("0.90");
    private static final int WELCOME_MAX_MONTHS = 1;

    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    /**
     * Calcula o valor final da cobrança.
     *
     * @param command dados da cobrança
     * @param today   data de referência do cálculo
     */
    public Money calculate(BillingCommand command, LocalDate today) {
        Objects.requireNonNull(command, "command não pode ser nulo");
        Objects.requireNonNull(today, "today não pode ser nulo");

        int monthsActive = monthsActive(command.signupDate(), today);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromoCode(price, command.promoCode(), monthsActive, today);
        price = applyLateFees(price, command.overdueDays());
        return price.atLeastZero();
    }

    /**
     * Meses de assinatura, com a mesma conta do legado: considera só ano e mês,
     * sem olhar o dia (31/01 a 01/02 conta 1 mês).
     */
    static int monthsActive(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());
    }

    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > LONG_LOYALTY_MONTHS) {
            return basePrice.minus(basePrice.times(LONG_LOYALTY_DISCOUNT));
        }
        if (monthsActive > SHORT_LOYALTY_MONTHS) {
            return basePrice.minus(basePrice.times(SHORT_LOYALTY_DISCOUNT));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.times(NON_PROFIT_FACTOR) : price;
    }

    private Money applyPromoCode(Money price, String promoCode, int monthsActive, LocalDate today) {
        return PromoCode.from(promoCode)
                .map(promo -> switch (promo) {
                    case BLACKFRIDAY -> today.getMonth() == BLACK_FRIDAY_MONTH
                            ? price.minus(BLACK_FRIDAY_DISCOUNT)
                            : price;
                    case WELCOME10 -> monthsActive <= WELCOME_MAX_MONTHS
                            ? price.times(WELCOME_FACTOR)
                            : price;
                })
                .orElse(price);
    }

    private Money applyLateFees(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.times(LATE_PENALTY_RATE);
        Money interest = price.times(DAILY_INTEREST_RATE.multiply(BigDecimal.valueOf(overdueDays)));
        return price.plus(penalty).plus(interest);
    }
}
