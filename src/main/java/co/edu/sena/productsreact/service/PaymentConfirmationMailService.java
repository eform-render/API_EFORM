package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.PaymentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class PaymentConfirmationMailService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.mail.from:noreply@eform.com}")
    private String fromAddress;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    // Async: el checkout no debe esperar a que Brevo responda para confirmarle el pago al cliente.
    @Async("taskExecutor")
    public void sendPaymentConfirmation(PaymentRecord payment) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.out.println("BREVO API KEY no configurado. Email no enviado.");
            return;
        }

        if (payment.getPaymentReferenceCode() == null) {
            return;
        }

        String html = buildEmailContent(payment);

        try {
            sendEmail(payment.getCustomerEmail(),
                    "Confirmación de tu pedido - EFORM #" + payment.getId(),
                    html);
        } catch (Exception e) {
            System.err.println("Error enviando email de confirmación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildEmailContent(PaymentRecord payment) {
        String currencyFormat = String.format("$%,.0f COP", payment.getAmount());

        return """
                <h2>Confirmación de Pedido - EFORM</h2>
                <p>Hola %s,</p>
                <p>Tu pedido ha sido registrado correctamente. A continuación encontrarás los detalles:</p>

                <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p><strong>Número de Pedido:</strong> #%d</p>
                    <p><strong>Código de Referencia:</strong> <span style="color: #007bff; font-weight: bold; font-size: 16px;">%s</span></p>
                    <p><strong>Monto:</strong> %s</p>
                    <p><strong>Método de Pago:</strong> %s</p>
                    <p><strong>Estado:</strong> Pendiente de confirmación</p>
                </div>

                <p><strong>Importante:</strong> Utiliza el código de referencia <strong>%s</strong> para realizar tu pago en la sede.</p>

                <p>Cuando confirmemos tu pago, recibirás un email con la factura del pedido.</p>
                <p>Si tienes preguntas, contáctanos.</p>
                <p>Gracias por tu compra en EFORM.</p>
                """.formatted(
                payment.getCustomerName(),
                payment.getId(),
                payment.getPaymentReferenceCode(),
                currencyFormat,
                payment.getPaymentMethod(),
                payment.getPaymentReferenceCode()
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
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.brevo.com/v3/smtp/email",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            System.out.println("EMAIL DE CONFIRMACIÓN ENVIADO - STATUS: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("ERROR ENVIANDO EMAIL DE CONFIRMACIÓN: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
