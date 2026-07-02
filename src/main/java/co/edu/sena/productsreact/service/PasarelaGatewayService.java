package co.edu.sena.productsreact.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * Cliente para la pasarela de pagos externa (API de ADSO454/api-pasarela).
 * Crea pagos con comprobante y sincroniza aprobaciones/rechazos.
 */
@Service
public class PasarelaGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PasarelaGatewayService.class);

    private static final Set<String> METODOS_CON_PASARELA = Set.of("Nequi", "Transferencia BRE-BE");

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${pasarela.api-url}")
    private String apiUrl;

    @Value("${pasarela.api-key}")
    private String apiKey;

    public boolean requiereComprobante(String paymentMethod) {
        return METODOS_CON_PASARELA.contains(paymentMethod);
    }

    private String mapMetodoPago(String paymentMethod) {
        return switch (paymentMethod) {
            case "Nequi" -> "NEQUI";
            case "Transferencia BRE-BE" -> "BRE_B";
            default -> null;
        };
    }

    /**
     * Crea un pago en la pasarela externa con el comprobante adjunto.
     * Devuelve el id (UUID) del pago creado alla, o null si no se pudo crear.
     */
    public String crearPago(Long ordenId, String paymentMethod, Double amount, String customerEmail, MultipartFile comprobante) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("PASARELA_API_KEY no configurado. No se registrara el pago en la pasarela externa para orden={}", ordenId);
            return null;
        }

        String metodoPago = mapMetodoPago(paymentMethod);
        if (metodoPago == null) {
            return null;
        }

        try {
            Map<String, Object> datos = Map.of(
                    "monto", amount,
                    "moneda", "COP",
                    "metodoPago", metodoPago,
                    "idCliente", customerEmail,
                    "descripcion", "Pedido EFORM #" + ordenId,
                    "referencia", "EFORM-" + ordenId
            );

            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(
                    comprobante.getContentType() != null ? comprobante.getContentType() : MediaType.IMAGE_JPEG_VALUE));
            HttpEntity<ByteArrayResource> imagenPart = new HttpEntity<>(
                    new ByteArrayResource(comprobante.getBytes()) {
                        @Override
                        public String getFilename() {
                            return comprobante.getOriginalFilename();
                        }
                    },
                    fileHeaders
            );

            HttpHeaders datosHeaders = new HttpHeaders();
            datosHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> datosPart = new HttpEntity<>(objectMapper.writeValueAsString(datos), datosHeaders);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("datos", datosPart);
            body.add("imagen", imagenPart);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-API-Key", apiKey);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/api/pagos",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Object id = response.getBody() != null ? response.getBody().get("id") : null;
            log.info("Pago creado en pasarela externa para orden={}: id={}", ordenId, id);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            log.error("Error creando pago en pasarela externa para orden={}: {}", ordenId, e.getMessage(), e);
            return null;
        }
    }

    public ResponseEntity<byte[]> descargarComprobante(String externalPaymentId) {
        if (externalPaymentId == null || externalPaymentId.isBlank() || apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    apiUrl + "/api/pagos/" + externalPaymentId + "/imagen",
                    HttpMethod.GET,
                    request,
                    byte[].class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(
                    response.getHeaders().getContentType() != null
                            ? response.getHeaders().getContentType()
                            : MediaType.IMAGE_JPEG
            );

            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
        } catch (Exception e) {
            log.error("Error al descargar comprobante {} de la pasarela externa: {}", externalPaymentId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    public void aprobarPago(String externalPaymentId) {
        cambiarEstado(externalPaymentId, "aprobar");
    }

    public void rechazarPago(String externalPaymentId) {
        cambiarEstado(externalPaymentId, "rechazar");
    }

    private void cambiarEstado(String externalPaymentId, String accion) {
        if (externalPaymentId == null || externalPaymentId.isBlank() || apiKey == null || apiKey.isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    apiUrl + "/api/pagos/" + externalPaymentId + "/" + accion,
                    HttpMethod.PUT,
                    request,
                    String.class
            );
            log.info("Pago {} marcado como '{}' en la pasarela externa", externalPaymentId, accion);
        } catch (Exception e) {
            log.error("Error al {} el pago {} en la pasarela externa: {}", accion, externalPaymentId, e.getMessage(), e);
        }
    }
}
