package com.saas.billing.port;

import com.saas.billing.domain.Customer;
import com.saas.billing.port.exception.InfrastructureException;

import java.util.Optional;

/** Porta de acesso aos dados dos clientes. */
public interface CustomerRepository {

    /**
     * Busca o cliente pelo identificador.
     *
     * @return o cliente, ou vazio se não existir
     * @throws InfrastructureException se o acesso aos dados falhar
     */
    Optional<Customer> findById(String id);
}
