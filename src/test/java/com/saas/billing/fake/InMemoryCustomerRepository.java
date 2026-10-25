package com.saas.billing.fake;

import com.saas.billing.domain.Customer;
import com.saas.billing.port.CustomerRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Repositório de clientes em memória, para testar o serviço sem banco de dados. */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers = new HashMap<>();

    public InMemoryCustomerRepository add(Customer customer) {
        customers.put(customer.id(), customer);
        return this;
    }

    @Override
    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(customers.get(id));
    }
}
