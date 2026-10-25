package com.saas.billing.adapter.jdbc;

import com.saas.billing.domain.Customer;
import com.saas.billing.domain.CustomerStatus;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.exception.InfrastructureException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Busca clientes na tabela {@code customers} via JDBC. */
public final class JdbcCustomerRepository implements CustomerRepository {

    private static final String FIND_BY_ID = "SELECT email, status FROM customers WHERE id = ?";

    private final JdbcConnectionFactory connections;

    public JdbcCustomerRepository(JdbcConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections não pode ser nulo");
    }

    @Override
    public Optional<Customer> findById(String id) {
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Customer(
                        id,
                        resultSet.getString("email"),
                        CustomerStatus.from(resultSet.getString("status"))));
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Falha ao buscar o cliente " + id, e);
        }
    }
}
