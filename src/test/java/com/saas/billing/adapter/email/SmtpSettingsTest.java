package com.saas.billing.adapter.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmtpSettingsTest {

    private static Map<String, String> ambienteCompleto() {
        Map<String, String> env = new HashMap<>();
        env.put("BILLING_SMTP_HOST", "smtp.empresa.com");
        env.put("BILLING_SMTP_PORT", "587");
        env.put("BILLING_SMTP_FROM", "billing@empresa.com");
        return env;
    }

    @Test
    @DisplayName("from: lê servidor, porta e remetente quando as variáveis estão definidas")
    void from_leServidorPortaERemetente_quandoVariaveisDefinidas() {
        SmtpSettings settings = SmtpSettings.from(ambienteCompleto()::get);

        assertEquals(new SmtpSettings("smtp.empresa.com", 587, "billing@empresa.com"), settings);
    }

    @Test
    @DisplayName("from: lança erro com o nome da variável quando ela está ausente")
    void from_lancaErroComNomeDaVariavel_quandoVariavelAusente() {
        Map<String, String> env = ambienteCompleto();
        env.remove("BILLING_SMTP_HOST");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> SmtpSettings.from(env::get));

        assertEquals("Variável de ambiente BILLING_SMTP_HOST não definida", erro.getMessage());
    }

    @Test
    @DisplayName("from: lança erro com o nome da variável quando ela está em branco")
    void from_lancaErroComNomeDaVariavel_quandoVariavelEmBranco() {
        Map<String, String> env = ambienteCompleto();
        env.put("BILLING_SMTP_FROM", "   ");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> SmtpSettings.from(env::get));

        assertEquals("Variável de ambiente BILLING_SMTP_FROM não definida", erro.getMessage());
    }

    @Test
    @DisplayName("from: lança erro claro quando a porta não é numérica")
    void from_lancaErroClaro_quandoPortaNaoENumerica() {
        Map<String, String> env = ambienteCompleto();
        env.put("BILLING_SMTP_PORT", "abc");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> SmtpSettings.from(env::get));

        assertEquals("Variável de ambiente BILLING_SMTP_PORT deve ser um número: abc", erro.getMessage());
    }
}
