package com.saas.billing.adapter.email;

import com.saas.billing.domain.Money;
import com.saas.billing.port.exception.InfrastructureException;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonsEmailNotificationServiceTest {

    private final CommonsEmailNotificationService service = new CommonsEmailNotificationService(
            new SmtpSettings("smtp.empresa.com", 587, "billing@empresa.com"));

    @Test
    @DisplayName("Monta o e-mail com a configuração, o assunto e o destinatário do legado")
    void montaOEmail() throws EmailException {
        SimpleEmail email = service.buildInvoiceEmail("cliente@exemplo.com", Money.of("59.50"));

        assertEquals("smtp.empresa.com", email.getHostName());
        assertEquals("587", email.getSmtpPort());
        assertEquals("billing@empresa.com", email.getFromAddress().getAddress());
        assertEquals("Sua Fatura Mensal", email.getSubject());
        assertEquals("cliente@exemplo.com", email.getToAddresses().get(0).getAddress());
    }

    @Test
    @DisplayName("O texto do e-mail traz o valor formatado em reais")
    void textoComValorFormatado() {
        assertEquals("Olá! Sua fatura foi processada no valor de R$ 59,50",
                CommonsEmailNotificationService.invoiceText(Money.of("59.50")));
    }

    @Test
    @DisplayName("Com endereço válido, não lança erro e não precisa de servidor SMTP")
    void naoEnviaNemPrecisaDeServidor() {
        assertDoesNotThrow(() -> service.sendInvoice("cliente@exemplo.com", Money.of("59.50")));
    }

    @Test
    @DisplayName("Endereço inválido vira InfrastructureException com a causa original")
    void enderecoInvalidoViraErroDeInfraestrutura() {
        InfrastructureException erro = assertThrows(InfrastructureException.class,
                () -> service.sendInvoice("", Money.of("59.50")));

        assertInstanceOf(EmailException.class, erro.getCause());
    }
}
