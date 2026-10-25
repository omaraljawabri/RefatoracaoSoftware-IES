package com.saas.billing.domain;

import java.util.Objects;

/**
 * Cliente cobrado, com os dados que o código legado lia da tabela {@code customers}.
 *
 * @param id     identificador do cliente
 * @param email  e-mail para onde vai o recibo
 * @param status situação cadastral do cliente
 */
public record Customer(String id, String email, CustomerStatus status) {

    public Customer {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(status, "status não pode ser nulo");
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
}
