package com.saas.billing.config;

import com.saas.billing.adapter.email.SmtpSettings;
import com.saas.billing.adapter.jdbc.JdbcConnectionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BillingServiceFactoryTest {

    @Test
    @DisplayName("create: monta o serviço sem abrir conexão nem servidor SMTP quando recebe a configuração")
    void create_montaServicoSemAcessarInfraestrutura_quandoRecebeConfiguracao() {
        assertNotNull(BillingServiceFactory.create(
                new JdbcConnectionFactory("jdbc:postgresql://localhost:5432/saas_db", "admin", "segredo"),
                new SmtpSettings("smtp.empresa.com", 587, "billing@empresa.com"),
                Clock.systemUTC()));
    }
}
