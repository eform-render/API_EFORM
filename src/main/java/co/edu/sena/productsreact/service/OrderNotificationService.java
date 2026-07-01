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
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private InvoiceMailService invoiceMailService;

    @Value("${app.mail.from:noreply@eform.com}")
    private String fromAddress;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    public void notifyOrderStatusChange(PaymentRecord payment) {
        log.info("Iniciando notificación de cambio de estado para payment={}, status={}", payment.getId(), payment.getStatus());

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO API KEY no configurado. Email no enviado para payment={}", payment.getId());
            return;
        }

        try {
            if ("ENVIADO".equals(payment.getStatus())) {
                log.info("Estado ENVIADO detectado para payment={}. Enviando factura...", payment.getId());
                invoiceMailService.sendInvoice(payment);
                return;
            }

            String statusLabel = getStatusLabel(payment.getStatus());
            String html = buildEmailContent(payment, statusLabel);

            try {
                sendEmail(payment.getCustomerEmail(), "Cambio de estado en tu pedido #" + payment.getId(), html);
            } catch (Exception e) {
                log.error("Error enviando email de notificación para payment={}: {}", payment.getId(), e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error en notificación de cambio de estado para payment={}: {}", payment.getId(), e.getMessage(), e);
        }
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "PENDIENTE" -> "Pendiente";
            case "CONFIRMADO" -> "Confirmado";
            case "ENVIADO" -> "Enviado";
            case "COMPLETADO" -> "Completado";
            case "CANCELADO" -> "Cancelado";
            default -> status;
        };
    }

    private String buildEmailContent(PaymentRecord payment, String statusLabel) {
        String observationHtml = payment.getObservation() != null && !payment.getObservation().isBlank()
                ? "<p><strong>Observación:</strong> " + payment.getObservation() + "</p>"
                : "";

        return """
                <h2>Notificación de cambio de estado - EFORM</h2>
                <p>Hola %s,</p>
                <p>El estado de tu pedido ha cambiado.</p>
                <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p><strong>Número de Pedido:</strong> #%d</p>
                    <p><strong>Nuevo Estado:</strong> <span style="color: #007bff; font-weight: bold;">%s</span></p>
                    <p><strong>Monto:</strong> $%,.0f COP</p>
                    <p><strong>Método de Pago:</strong> %s</p>
                    %s
                </div>
                <p>Si tienes preguntas sobre tu pedido, por favor contáctanos.</p>
                <p>Gracias por tu compra en EFORM.</p>
                """.formatted(
                payment.getCustomerName(),
                payment.getId(),
                statusLabel,
                payment.getAmount(),
                payment.getPaymentMethod(),
                observationHtml
        );
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
            log.debug("Enviando notificación de estado a {}", toEmail);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.brevo.com/v3/smtp/email",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Notificación de estado enviada exitosamente a {} - Status: {}", toEmail, response.getStatusCode());
        } catch (Exception e) {
            log.error("Error enviando notificación a {}: {} - {}", toEmail, e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
