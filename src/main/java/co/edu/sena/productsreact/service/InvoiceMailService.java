package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.PaymentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class InvoiceMailService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceMailService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private InvoiceService invoiceService;

    @Value("${app.mail.from:noreply@eform.com}")
    private String fromAddress;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    public void sendInvoice(PaymentRecord payment) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO API KEY no configurado. Email de factura no enviado para payment={}", payment.getId());
            return;
        }

        log.info("Iniciando envío de factura para payment={}", payment.getId());

        try {
            log.debug("Generando HTML de factura para payment={}", payment.getId());
            String invoiceHtml = invoiceService.generateInvoiceHtml(payment);
            log.debug("HTML de factura generado exitosamente para payment={}", payment.getId());

            try {
                sendEmail(payment.getCustomerEmail(),
                        "Factura de tu pedido #" + payment.getId() + " - EFORM",
                        invoiceHtml);
                log.info("Factura enviada exitosamente a {} para payment={}", payment.getCustomerEmail(), payment.getId());
            } catch (Exception e) {
                log.error("Error enviando factura para payment={}: {}", payment.getId(), e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error generando HTML de factura para payment={}: {}", payment.getId(), e.getMessage(), e);
        }
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", "EFORM",
                        "email", fromAddress
                ),
                "to", List.of(
                        Map.of("email", toEmail)
                ),
                "subject", subject,
                "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.debug("Enviando email a {} con asunto: {}", toEmail, subject);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.brevo.com/v3/smtp/email",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Email enviado exitosamente a {} - Status: {}", toEmail, response.getStatusCode());
        } catch (Exception e) {
            log.error("Error enviando email a {}: {} - {}", toEmail, e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
