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
    @DisplayName("Lê servidor, porta e remetente das variáveis de ambiente")
    void leAsVariaveis() {
        SmtpSettings settings = SmtpSettings.from(ambienteCompleto()::get);

        assertEquals(new SmtpSettings("smtp.empresa.com", 587, "billing@empresa.com"), settings);
    }

    @Test
    @DisplayName("Variável ausente gera erro com o nome dela")
    void variavelAusente() {
        Map<String, String> env = ambienteCompleto();
        env.remove("BILLING_SMTP_HOST");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> SmtpSettings.from(env::get));

        assertEquals("Variável de ambiente BILLING_SMTP_HOST não definida", erro.getMessage());
    }

    @Test
    @DisplayName("Variável em branco gera erro com o nome dela")
    void variavelEmBranco() {
        Map<String, String> env = ambienteCompleto();
        env.put("BILLING_SMTP_FROM", "   ");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> SmtpSettings.from(env::get));

        assertEquals("Variável de ambiente BILLING_SMTP_FROM não definida", erro.getMessage());
    }

    @Test
    @DisplayName("Porta que não é número gera erro claro")
    void portaInvalida() {
        Map<String, String> env = ambienteCompleto();
        env.put("BILLING_SMTP_PORT", "abc");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> SmtpSettings.from(env::get));

        assertEquals("Variável de ambiente BILLING_SMTP_PORT deve ser um número: abc", erro.getMessage());
    }
}
