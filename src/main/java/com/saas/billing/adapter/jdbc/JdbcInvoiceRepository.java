package com.saas.billing.adapter.jdbc;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.exception.InfrastructureException;
import com.saas.billing.port.InvoiceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/** Grava faturas na tabela {@code invoices} via JDBC. */
public final class JdbcInvoiceRepository implements InvoiceRepository {

    private static final String INSERT =
            "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";

    private final JdbcConnectionFactory connections;

    public JdbcInvoiceRepository(JdbcConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections não pode ser nulo");
    }

    @Override
    public void save(Invoice invoice) {
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, invoice.customerId());
            statement.setBigDecimal(2, invoice.amount().amount());
            statement.setString(3, invoice.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException("Falha ao gravar a fatura do cliente " + invoice.customerId(), e);
        }
    }
}
