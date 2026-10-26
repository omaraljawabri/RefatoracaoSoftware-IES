package com.saas.billing.port;

import com.saas.billing.domain.Money;

/**
 * Porta para o envio de notificações ao cliente.
 *
 * <p>O serviço de cobrança depende só desta interface; o envio por e-mail fica
 * em um adaptador e, nos testes, é trocado por um fake que só registra as
 * mensagens.</p>
 */
public interface NotificationService {

    /**
     * Notifica o cliente de que a fatura foi processada.
     *
     * @param email  endereço do cliente
     * @param amount valor da fatura
     * @throws com.saas.billing.port.exception.InfrastructureException se a
     *         notificação não puder ser montada ou enviada
     */
    void sendInvoice(String email, Money amount);
}
