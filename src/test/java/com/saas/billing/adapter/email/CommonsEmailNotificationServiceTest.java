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
    @DisplayName("buildInvoiceEmail: preenche servidor, remetente, assunto e destinatário quando usa a configuração do legado")
    void buildInvoiceEmail_preencheServidorRemetenteAssuntoEDestinatario_quandoUsaConfiguracaoDoLegado() throws EmailException {
        SimpleEmail email = service.buildInvoiceEmail("cliente@exemplo.com", Money.of("59.50"));

        assertEquals("smtp.empresa.com", email.getHostName());
        assertEquals("587", email.getSmtpPort());
        assertEquals("billing@empresa.com", email.getFromAddress().getAddress());
        assertEquals("Sua Fatura Mensal", email.getSubject());
        assertEquals("cliente@exemplo.com", email.getToAddresses().get(0).getAddress());
    }

    @Test
    @DisplayName("invoiceText: inclui o valor formatado em reais quando recebe um Money")
    void invoiceText_incluiValorEmReais_quandoRecebeMoney() {
        assertEquals("Olá! Sua fatura foi processada no valor de R$ 59,50",
                CommonsEmailNotificationService.invoiceText(Money.of("59.50")));
    }

    @Test
    @DisplayName("sendInvoice: não lança erro nem acessa o servidor SMTP quando o endereço é válido")
    void sendInvoice_naoLancaErroNemAcessaServidor_quandoEnderecoValido() {
        assertDoesNotThrow(() -> service.sendInvoice("cliente@exemplo.com", Money.of("59.50")));
    }

    @Test
    @DisplayName("sendInvoice: lança InfrastructureException com a causa original quando o endereço é inválido")
    void sendInvoice_lancaInfrastructureException_quandoEnderecoInvalido() {
        InfrastructureException erro = assertThrows(InfrastructureException.class,
                () -> service.sendInvoice("", Money.of("59.50")));

        assertInstanceOf(EmailException.class, erro.getCause());
    }
}
