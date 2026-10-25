package com.saas.billing.port;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.exception.InfrastructureException;

/** Porta de gravação das faturas geradas pela cobrança. */
public interface InvoiceRepository {

    /**
     * Grava a fatura.
     *
     * @throws InfrastructureException se a gravação falhar
     */
    void save(Invoice invoice);
}
