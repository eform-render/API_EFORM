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

import java.util.List;
import java.util.Map;

@Service
public class InvoiceMailService {

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
            System.out.println("BREVO API KEY no configurado. Email no enviado.");
            return;
        }

        try {
            String invoiceHtml = invoiceService.generateInvoiceHtml(payment);

            try {
                sendEmail(payment.getCustomerEmail(),
                        "Factura de tu pedido #" + payment.getId() + " - EFORM",
                        invoiceHtml);
            } catch (Exception e) {
                System.err.println("Error enviando factura: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error generando HTML de factura: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("========== ENVIANDO FACTURA ==========");
            System.out.println("TO: " + toEmail);
            System.out.println("SUBJECT: " + subject);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.brevo.com/v3/smtp/email",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            System.out.println("FACTURA ENVIADA OK - STATUS: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("ERROR ENVIANDO FACTURA: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
