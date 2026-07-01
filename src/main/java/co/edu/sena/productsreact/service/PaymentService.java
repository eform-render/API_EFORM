package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.payment.PaymentRequest;
import co.edu.sena.productsreact.entity.PaymentRecord;
import co.edu.sena.productsreact.repository.PaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRecordRepository paymentRecordRepository;
    private final co.edu.sena.productsreact.repository.ReservationRepository reservationRepository;
    private final OrderNotificationService orderNotificationService;
    private final PaymentReferenceService paymentReferenceService;
    private final PaymentConfirmationMailService paymentConfirmationMailService;

    @Transactional
    public PaymentRecord save(PaymentRequest request) {
        PaymentRecord record = new PaymentRecord(
                request.customerName(),
                request.customerEmail(),
                request.paymentMethod(),
                request.amount(),
                LocalDateTime.now()
        );

        if ("pago en sede".equalsIgnoreCase(request.paymentMethod())) {
            record.setPaymentReferenceCode(paymentReferenceService.generatePaymentReference());
        }

        PaymentRecord saved = paymentRecordRepository.save(record);

        // Vincular Reservations del cliente con este pago
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        var reservations = reservationRepository.findByReservedByAndCreatedAtAfter(
            request.customerEmail(), fiveMinutesAgo
        );

        for (var reservation : reservations) {
            if (reservation.getPayment() == null) {
                reservation.setPayment(saved);
                reservationRepository.save(reservation);
            }
        }

        if (saved.getPaymentReferenceCode() != null) {
            paymentConfirmationMailService.sendPaymentConfirmation(saved);
        }

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
    public PaymentRecord updateStatus(Long id, String newStatus, String observation) {
        PaymentRecord payment = paymentRecordRepository.findById(id)
                .orElseThrow(() -> new co.edu.sena.productsreact.exception.ResourceNotFoundException("Pedido no encontrado"));
        payment.setStatus(newStatus);
        if (observation != null && !observation.isBlank()) {
            payment.setObservation(observation);
        }
        PaymentRecord updated = paymentRecordRepository.save(payment);

        // Enviar email de notificación
        orderNotificationService.notifyOrderStatusChange(updated);

        return updated;
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

        var reservations = reservationRepository.findByPaymentId(paymentId);

        java.util.List<co.edu.sena.productsreact.dto.payment.OrderDetailsResponse.OrderItemDTO> items =
            reservations.stream()
                .map(r -> new co.edu.sena.productsreact.dto.payment.OrderDetailsResponse.OrderItemDTO(
                    r.getProduct().getId(),
                    r.getProduct().getNombre(),
                    r.getQuantity(),
                    r.getProduct().getPrecio()
                ))
                .toList();

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

}
