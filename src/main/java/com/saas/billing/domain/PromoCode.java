package com.saas.billing.domain;

import java.util.Optional;

/**
 * Cupons promocionais aceitos na cobrança.
 *
 * Substitui as comparações de texto do código legado. As condições de cada
 * cupom (mês de novembro, meses de assinatura) ficam com o cálculo de preço.
 */
public enum PromoCode {
    BLACKFRIDAY,
    WELCOME10;

    /**
     * Reconhece o cupom do mesmo jeito que o código legado: sem diferenciar
     * maiúsculas de minúsculas e sem remover espaços. Cupom nulo, em branco
     * ou desconhecido não é reconhecido.
     */
    public static Optional<PromoCode> from(String code) {
        for (PromoCode promoCode : values()) {
            if (promoCode.name().equalsIgnoreCase(code)) {
                return Optional.of(promoCode);
            }
        }
        return Optional.empty();
    }
}
