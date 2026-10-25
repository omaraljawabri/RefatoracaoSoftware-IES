package com.saas.billing.domain;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/**
 * Valor monetário imutável, sempre com 2 casas decimais.
 *
 * Concentra num só lugar a regra de arredondamento, que no código legado
 * não existia (os valores eram {@code double} e geravam resultados como
 * {@code 59.49999999999999})
 *
 * Toda operação devolve um novo {@code Money}; o objeto original nunca muda;
 */

public record Money(BigDecimal amount) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /** Normaliza o valor para 2 casas, para que 10 e 10.00 sejam iguais. */
    public Money {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    /** Cria a partir de um {@code double}, como os valores recebidos hoje pelo serviço. */
    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    /** Cria a partir de um texto exato, como {@code "59.50"}. Útil em testes e constantes. */
    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Multiplica por um fator, como {@code 0.70} para 30% de desconto. */
    public Money times(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor não pode ser nulo");
        return new Money(amount.multiply(factor));
    }

    /** Devolve zero se o valor for negativo; caso contrário, o próprio valor. */
    public Money atLeastZero() {
        return amount.signum() < 0 ? ZERO : this;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /** Formata no padrão brasileiro, por exemplo {@code "R$ 1.234,56"}. */
    public String formatted() {
        return "R$ " + String.format(PT_BR, "%,.2f", amount);
    }
}