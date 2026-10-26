package com.saas.billing.adapter.email;

import java.util.Objects;
import java.util.function.Function;

/**
 * Configuração do servidor de e-mail, lida de variáveis de ambiente.
 *
 * <p>Substitui os valores fixos no código legado (linhas 90–92):</p>
 * <ul>
 *   <li>{@code BILLING_SMTP_HOST}: servidor SMTP (legado: {@code smtp.empresa.com})</li>
 *   <li>{@code BILLING_SMTP_PORT}: porta SMTP (legado: {@code 587})</li>
 *   <li>{@code BILLING_SMTP_FROM}: remetente (legado: {@code billing@empresa.com})</li>
 * </ul>
 */
public record SmtpSettings(String host, int port, String from) {

    static final String HOST_VARIABLE = "BILLING_SMTP_HOST";
    static final String PORT_VARIABLE = "BILLING_SMTP_PORT";
    static final String FROM_VARIABLE = "BILLING_SMTP_FROM";

    public SmtpSettings {
        Objects.requireNonNull(host, "host não pode ser nulo");
        Objects.requireNonNull(from, "from não pode ser nulo");
    }

    /** Lê a configuração das variáveis de ambiente do sistema. */
    public static SmtpSettings fromEnvironment() {
        return from(System::getenv);
    }

    /**
     * Lê a configuração a partir de uma função de consulta de variáveis.
     * Existe para que os testes não dependam das variáveis da máquina.
     */
    static SmtpSettings from(Function<String, String> environment) {
        String host = required(environment, HOST_VARIABLE);
        String port = required(environment, PORT_VARIABLE);
        String from = required(environment, FROM_VARIABLE);
        return new SmtpSettings(host, parsePort(port), from);
    }

    private static String required(Function<String, String> environment, String name) {
        String value = environment.apply(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value.trim();
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Variável de ambiente " + PORT_VARIABLE + " deve ser um número: " + value, e);
        }
    }
}
