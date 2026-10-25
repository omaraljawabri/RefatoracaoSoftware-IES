package com.saas.billing.port.exception;

/**
 * Falha num serviço externo (banco de dados, gateway de pagamento, e-mail).
 *
 * Os adaptadores convertem as exceções técnicas de cada biblioteca nesta
 * exceção, para que o serviço de cobrança não dependa de {@code SQLException}
 * nem de {@code EmailException}. Ela substitui o {@code catch (Exception e)}
 * do código legado, que escondia essas falhas numa mensagem de log.
 */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
