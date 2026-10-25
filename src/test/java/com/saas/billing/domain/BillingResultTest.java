package com.saas.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingResultTest {

    @Test
    @DisplayName("Guarda o valor e as mensagens na ordem recebida")
    void guardaValorEMensagens() {
        BillingResult resultado = new BillingResult(Money.of("59.50"),
                List.of("Pagamento processado com sucesso: R$ 59,50", "E-mail disparado para: a@b.com"));

        assertEquals(Money.of("59.50"), resultado.amount());
        assertEquals(List.of("Pagamento processado com sucesso: R$ 59,50", "E-mail disparado para: a@b.com"),
                resultado.messages());
    }

    @Test
    @DisplayName("Aceita lista vazia, como no caso de cliente não encontrado")
    void aceitaListaVazia() {
        BillingResult resultado = new BillingResult(Money.of(100), List.of());

        assertTrue(resultado.messages().isEmpty());
    }

    @Test
    @DisplayName("A lista de mensagens não pode ser alterada")
    void mensagensSaoImutaveis() {
        BillingResult resultado = new BillingResult(Money.of(100), List.of("mensagem"));

        assertThrows(UnsupportedOperationException.class, () -> resultado.messages().add("outra"));
    }

    @Test
    @DisplayName("Alterar a lista original depois não afeta o resultado")
    void fazCopiaDaListaRecebida() {
        List<String> logs = new ArrayList<>();
        logs.add("primeira");
        BillingResult resultado = new BillingResult(Money.of(100), logs);

        logs.add("segunda");

        assertEquals(List.of("primeira"), resultado.messages());
    }

    @Test
    @DisplayName("Não aceita valor nem lista nulos")
    void rejeitaNulos() {
        assertThrows(NullPointerException.class, () -> new BillingResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new BillingResult(Money.of(100), null));
    }

    @Test
    @DisplayName("Resultados com os mesmos dados são iguais")
    void resultadosComMesmosDadosSaoIguais() {
        BillingResult a = new BillingResult(Money.of(100), List.of("x"));
        BillingResult b = new BillingResult(Money.of("100.00"), new ArrayList<>(List.of("x")));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}