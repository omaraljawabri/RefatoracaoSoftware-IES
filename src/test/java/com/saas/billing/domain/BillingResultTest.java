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
    @DisplayName("constructor: guarda o valor e as mensagens na ordem quando recebe uma lista")
    void constructor_guardaValorEMensagensNaOrdem_quandoRecebeLista() {
        BillingResult resultado = new BillingResult(Money.of("59.50"),
                List.of("Pagamento processado com sucesso: R$ 59,50", "E-mail disparado para: a@b.com"));

        assertEquals(Money.of("59.50"), resultado.amount());
        assertEquals(List.of("Pagamento processado com sucesso: R$ 59,50", "E-mail disparado para: a@b.com"),
                resultado.messages());
    }

    @Test
    @DisplayName("constructor: aceita lista vazia quando não há mensagens")
    void constructor_aceitaListaVazia_quandoNaoHaMensagens() {
        BillingResult resultado = new BillingResult(Money.of(100), List.of());

        assertTrue(resultado.messages().isEmpty());
    }

    @Test
    @DisplayName("messages: lança UnsupportedOperationException quando a lista é alterada")
    void messages_lancaUnsupportedOperationException_quandoAlterada() {
        BillingResult resultado = new BillingResult(Money.of(100), List.of("mensagem"));

        assertThrows(UnsupportedOperationException.class, () -> resultado.messages().add("outra"));
    }

    @Test
    @DisplayName("constructor: copia a lista quando a lista original muda depois")
    void constructor_copiaLista_quandoListaOriginalMudaDepois() {
        List<String> logs = new ArrayList<>();
        logs.add("primeira");
        BillingResult resultado = new BillingResult(Money.of(100), logs);

        logs.add("segunda");

        assertEquals(List.of("primeira"), resultado.messages());
    }

    @Test
    @DisplayName("constructor: lança NullPointerException quando o valor ou a lista são nulos")
    void constructor_lancaNullPointerException_quandoValorOuListaNulos() {
        assertThrows(NullPointerException.class, () -> new BillingResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new BillingResult(Money.of(100), null));
    }

    @Test
    @DisplayName("equals: retorna true e o mesmo hashCode quando os dados são iguais")
    void equals_retornaTrue_quandoMesmosDados() {
        BillingResult a = new BillingResult(Money.of(100), List.of("x"));
        BillingResult b = new BillingResult(Money.of("100.00"), new ArrayList<>(List.of("x")));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}