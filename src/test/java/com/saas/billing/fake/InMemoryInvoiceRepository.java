package com.saas.billing.fake;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.InvoiceRepository;

import java.util.ArrayList;
import java.util.List;

/** Repositório de faturas em memória, para testar o serviço sem banco de dados. */
public class InMemoryInvoiceRepository implements InvoiceRepository {

    private final List<Invoice> invoices = new ArrayList<>();

    @Override
    public void save(Invoice invoice) {
        invoices.add(invoice);
    }

    /** Faturas gravadas, na ordem em que foram salvas. */
    public List<Invoice> saved() {
        return List.copyOf(invoices);
    }
}
