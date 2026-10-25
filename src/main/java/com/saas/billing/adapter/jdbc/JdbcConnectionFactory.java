package com.saas.billing.adapter.jdbc;

import com.saas.billing.port.exception.InfrastructureException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * Abre conexões com o banco de dados.
 *
 * Substitui as credenciais fixas no código legado ({@code DB_URL},
 * {@code DB_USER}, {@code DB_PASS}), que agora vêm de variáveis de ambiente.
 */
public final class JdbcConnectionFactory {

    static final String URL_VARIABLE = "BILLING_DB_URL";
    static final String USER_VARIABLE = "BILLING_DB_USER";
    static final String PASSWORD_VARIABLE = "BILLING_DB_PASSWORD";

    private final String url;
    private final String user;
    private final String password;

    public JdbcConnectionFactory(String url, String user, String password) {
        this.url = Objects.requireNonNull(url, "url não pode ser nula");
        this.user = Objects.requireNonNull(user, "user não pode ser nulo");
        this.password = Objects.requireNonNull(password, "password não pode ser nula");
    }

    /** Lê a URL, o usuário e a senha das variáveis {@code BILLING_DB_*}. */
    public static JdbcConnectionFactory fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static JdbcConnectionFactory fromEnvironment(Map<String, String> environment) {
        return new JdbcConnectionFactory(
                required(environment, URL_VARIABLE),
                required(environment, USER_VARIABLE),
                required(environment, PASSWORD_VARIABLE));
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }

    /**
     * Abre uma nova conexão; quem chama deve fechá-la.
     *
     * @throws InfrastructureException se não for possível conectar
     */
    public Connection open() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new InfrastructureException("Não foi possível conectar ao banco de dados", e);
        }
    }
}
