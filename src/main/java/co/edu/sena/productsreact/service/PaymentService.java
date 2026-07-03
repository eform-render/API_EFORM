package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.payment.PaymentRequest;
import co.edu.sena.productsreact.entity.PaymentRecord;
import co.edu.sena.productsreact.event.ComprobanteUploadRequestedEvent;
import co.edu.sena.productsreact.repository.PaymentRecordRepository;
import co.edu.sena.productsreact.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static co.edu.sena.productsreact.util.AppClock.nowBogota;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRecordRepository paymentRecordRepository;
    private final co.edu.sena.productsreact.repository.ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final OrderNotificationService orderNotificationService;
    private final PaymentReferenceService paymentReferenceService;
    private final PaymentConfirmationMailService paymentConfirmationMailService;
    private final PasarelaGatewayService pasarelaGatewayService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentRecord save(PaymentRequest request, MultipartFile comprobante) {
        PaymentRecord record = new PaymentRecord(
                request.customerName(),
                request.customerEmail(),
                request.paymentMethod(),
                request.amount(),
                nowBogota()
        );

        // Establecer método de entrega y costo de domicilio
        String deliveryMethod = request.deliveryMethod() != null ? request.deliveryMethod() : "domicilio";
        record.setDeliveryMethod(deliveryMethod);

        if ("recoge".equalsIgnoreCase(deliveryMethod)) {
            record.setShippingCost(0.0);
        } else {
            record.setShippingCost(5500.0);
        }

        if ("pago en sede".equalsIgnoreCase(request.paymentMethod())) {
            record.setPaymentReferenceCode(paymentReferenceService.generatePaymentReference());
        }

        PaymentRecord saved = paymentRecordRepository.save(record);

        // Vincular items del carrito con este pago y construir ademas una copia fija
        // (snapshot en JSON) que no depende de la tabla de reservas para mostrar
        // el detalle del pedido mas adelante (factura, historial de compras, etc.).
        List<Map<String, Object>> itemsSnapshot = new ArrayList<>();
        if (request.items() != null && !request.items().isEmpty()) {
            var reservations = request.items().stream().map(item -> {
                var reservation = new co.edu.sena.productsreact.entity.Reservation();
                var product = new co.edu.sena.productsreact.entity.Product();
                product.setId(item.productId());

                reservation.setProduct(product);
                reservation.setQuantity(item.quantity());
                reservation.setPayment(saved);
                reservation.setCreatedAt(nowBogota());
                reservation.setReservedBy(request.customerEmail());

                var fullProduct = productRepository.findById(item.productId()).orElse(null);
                if (fullProduct != null) {
                    Map<String, Object> snapshotItem = new java.util.LinkedHashMap<>();
                    snapshotItem.put("productId", fullProduct.getId());
                    snapshotItem.put("productName", fullProduct.getNombre());
                    snapshotItem.put("quantity", item.quantity());
                    snapshotItem.put("unitPrice", fullProduct.getPrecio());
                    itemsSnapshot.add(snapshotItem);
                }

                return reservation;
            }).toList();
            reservationRepository.saveAll(reservations);
        }

        try {
            saved.setItemsJson(objectMapper.writeValueAsString(itemsSnapshot));
            paymentRecordRepository.save(saved);
        } catch (Exception e) {
            log.error("Error guardando snapshot de items para orden={}: {}", saved.getId(), e.getMessage(), e);
        }

        if (saved.getPaymentReferenceCode() != null) {
            // Async: no bloquea la respuesta del checkout esperando a Brevo.
            paymentConfirmationMailService.sendPaymentConfirmation(saved);
        }

        if (pasarelaGatewayService.requiereComprobante(request.paymentMethod())) {
            if (comprobante == null || comprobante.isEmpty()) {
                log.warn("Metodo de pago {} requiere comprobante pero no se recibio ninguno para orden={}",
                        request.paymentMethod(), saved.getId());
            } else {
                // La subida a Cloudinary y el registro en la pasarela externa son llamadas HTTP
                // lentas; se publican como evento y se procesan en segundo plano (ver
                // PaymentAsyncService) despues de confirmada esta transaccion, para que el
                // cliente no tenga que esperarlas al pagar.
                try {
                    byte[] fileBytes = comprobante.getBytes();
                    eventPublisher.publishEvent(new ComprobanteUploadRequestedEvent(
                            saved.getId(),
                            request.paymentMethod(),
                            request.amount(),
                            request.customerEmail(),
                            fileBytes,
                            comprobante.getContentType(),
                            comprobante.getOriginalFilename()
                    ));
                } catch (java.io.IOException e) {
                    log.error("No se pudo leer el comprobante para orden={}: {}", saved.getId(), e.getMessage(), e);
                }
            }
        }

        notificationService.notifyAdmins(
                "NUEVO_PEDIDO",
                "Nuevo pedido recibido",
                "Pedido #" + saved.getId() + " de " + saved.getCustomerName() + " por $" + String.format("%,.0f", saved.getAmount()) + " via " + saved.getPaymentMethod(),
                saved.getId()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentRecord> listAll() {
        return paymentRecordRepository.findAll();
    }

    @Transactional
    public void deleteById(Long id) {
        try {
            paymentRecordRepository.deleteById(id);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            log.info("Intento de eliminar pago no existente id={}", id);
            throw new co.edu.sena.productsreact.exception.ResourceNotFoundException("Pago no encontrado: " + id);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.error("Fallo al eliminar pago id={}: DataIntegrityViolation", id, ex);
            throw new IllegalArgumentException("No se puede eliminar el pago por restricciones de integridad de la base de datos.");
        }
    }

    @Transactional
    public void deleteByIds(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        try {
            paymentRecordRepository.deleteAllById(ids);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.error("Fallo al eliminar pagos ids={}: DataIntegrityViolation", ids, ex);
            throw new IllegalArgumentException("No se pueden eliminar algunos pagos por restricciones de integridad de la base de datos.");
        }
    }

    @Transactional
    public PaymentRecord updateStatus(
            Long id,
            String newStatus,
            String observation,
            java.time.LocalDate estimatedDeliveryDate,
            String estimatedDeliveryTime
    ) {
        PaymentRecord payment = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new co.edu.sena.productsreact.exception.ResourceNotFoundException("Pedido no encontrado"));
        payment.setStatus(newStatus);
        if (observation != null && !observation.isBlank()) {
            payment.setObservation(observation);
        }
        if (estimatedDeliveryDate != null) {
            payment.setEstimatedDeliveryDate(estimatedDeliveryDate);
        }
        if (estimatedDeliveryTime != null && !estimatedDeliveryTime.isBlank()) {
            payment.setEstimatedDeliveryTime(estimatedDeliveryTime);
        }
        PaymentRecord updated = paymentRecordRepository.save(payment);

        // Sincronizar con la pasarela externa si el pago se creo alla
        if (updated.getExternalPaymentId() != null) {
            if ("CONFIRMADO".equals(newStatus)) {
                pasarelaGatewayService.aprobarPago(updated.getExternalPaymentId());
            } else if ("CANCELADO".equals(newStatus)) {
                pasarelaGatewayService.rechazarPago(updated.getExternalPaymentId());
            }
        }

        // Enviar email de notificación
        orderNotificationService.notifyOrderStatusChange(updated);

        return updated;
    }

    @Transactional(readOnly = true)
    public org.springframework.http.ResponseEntity<byte[]> getReceiptImage(Long id) {
        PaymentRecord payment = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new co.edu.sena.productsreact.exception.ResourceNotFoundException("Pedido no encontrado"));

        if (payment.getExternalPaymentId() == null) {
            throw new co.edu.sena.productsreact.exception.ResourceNotFoundException("Este pedido no tiene comprobante registrado en la pasarela");
        }

        return pasarelaGatewayService.descargarComprobante(payment.getExternalPaymentId());
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentRecord> getOrdersByEmail(String email) {
        return paymentRecordRepository.findByCustomerEmailOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public co.edu.sena.productsreact.dto.payment.OrderDetailsResponse getOrderDetails(Long paymentId, String userEmail) {
        PaymentRecord payment = paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> new co.edu.sena.productsreact.exception.ResourceNotFoundException("Pedido no encontrado"));

        if (!payment.getCustomerEmail().equals(userEmail)) {
            throw new IllegalArgumentException("No tienes permiso para ver este pedido");
        }

        java.util.List<co.edu.sena.productsreact.dto.payment.OrderDetailsResponse.OrderItemDTO> items = itemsFromSnapshot(payment);

        if (items.isEmpty()) {
            // Respaldo para pedidos creados antes de existir el snapshot en JSON
            var reservations = reservationRepository.findByPaymentId(paymentId);
            items = reservations.stream()
                    .map(r -> new co.edu.sena.productsreact.dto.payment.OrderDetailsResponse.OrderItemDTO(
                        r.getProduct().getId(),
                        r.getProduct().getNombre(),
                        r.getQuantity(),
                        r.getProduct().getPrecio().doubleValue()
                    ))
                    .toList();
        }

        return new co.edu.sena.productsreact.dto.payment.OrderDetailsResponse(
            payment.getId(),
            payment.getCustomerName(),
            payment.getCustomerEmail(),
            payment.getPaymentMethod(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getObservation(),
            payment.getPaymentReferenceCode(),
            payment.getCreatedAt(),
            items
        );
    }

    private java.util.List<co.edu.sena.productsreact.dto.payment.OrderDetailsResponse.OrderItemDTO> itemsFromSnapshot(PaymentRecord payment) {
        if (payment.getItemsJson() == null || payment.getItemsJson().isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(
                    payment.getItemsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
            return raw.stream()
                    .map(map -> new co.edu.sena.productsreact.dto.payment.OrderDetailsResponse.OrderItemDTO(
                            ((Number) map.get("productId")).longValue(),
                            (String) map.get("productName"),
                            ((Number) map.get("quantity")).intValue(),
                            ((Number) map.get("unitPrice")).doubleValue()
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Error leyendo snapshot de items para orden={}: {}", payment.getId(), e.getMessage(), e);
            return List.of();
        }
    }

}
