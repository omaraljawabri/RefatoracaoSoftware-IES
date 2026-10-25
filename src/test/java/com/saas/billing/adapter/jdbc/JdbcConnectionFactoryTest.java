package com.saas.billing.adapter.jdbc;

import com.saas.billing.port.exception.InfrastructureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcConnectionFactoryTest {

    private static Map<String, String> ambienteCompleto() {
        Map<String, String> ambiente = new HashMap<>();
        ambiente.put(JdbcConnectionFactory.URL_VARIABLE, "jdbc:postgresql://localhost:5432/saas_db");
        ambiente.put(JdbcConnectionFactory.USER_VARIABLE, "billing");
        ambiente.put(JdbcConnectionFactory.PASSWORD_VARIABLE, "segredo");
        return ambiente;
    }

    @Test
    @DisplayName("Cria a fábrica quando as três variáveis de ambiente estão definidas")
    void criaComAmbienteCompleto() {
        assertDoesNotThrow(() -> JdbcConnectionFactory.fromEnvironment(ambienteCompleto()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            JdbcConnectionFactory.URL_VARIABLE,
            JdbcConnectionFactory.USER_VARIABLE,
            JdbcConnectionFactory.PASSWORD_VARIABLE
    })
    @DisplayName("Falha com mensagem clara quando falta uma variável de ambiente")
    void falhaSemVariavel(String variavel) {
        Map<String, String> ambiente = ambienteCompleto();
        ambiente.remove(variavel);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> JdbcConnectionFactory.fromEnvironment(ambiente));
        assertTrue(erro.getMessage().contains(variavel));
    }

    @Test
    @DisplayName("Trata variável em branco como não definida")
    void variavelEmBrancoNaoConta() {
        Map<String, String> ambiente = ambienteCompleto();
        ambiente.put(JdbcConnectionFactory.PASSWORD_VARIABLE, "  ");

        assertThrows(IllegalStateException.class, () -> JdbcConnectionFactory.fromEnvironment(ambiente));
    }

    @Test
    @DisplayName("Falha de conexão vira InfrastructureException com a causa original")
    void falhaDeConexaoViraInfrastructureException() {
        JdbcConnectionFactory fabrica = new JdbcConnectionFactory("jdbc:inexistente:teste", "u", "p");

        InfrastructureException erro = assertThrows(InfrastructureException.class, fabrica::open);
        assertInstanceOf(SQLException.class, erro.getCause());
    }
}
