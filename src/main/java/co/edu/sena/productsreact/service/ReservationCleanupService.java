package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.Reservation;
import co.edu.sena.productsreact.entity.Product;
import co.edu.sena.productsreact.repository.ReservationRepository;
import co.edu.sena.productsreact.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static co.edu.sena.productsreact.util.AppClock.nowBogota;

@Service
@RequiredArgsConstructor
public class ReservationCleanupService {

    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    @Value("${app.reservation.ttl-minutes:15}")
    private long ttlMinutes;

    @Scheduled(fixedDelayString = "${app.reservation.cleanup-ms:60000}")
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime cutoff = nowBogota().minusMinutes(ttlMinutes);
        // Solo se limpian reservas sin pago asociado (carritos/checkouts abandonados).
        // Las reservas ya vinculadas a un pago (payment != null) son el historial
        // de productos del pedido y no deben eliminarse nunca.
        List<Reservation> expired = reservationRepository.findByCreatedAtBeforeAndPaymentIsNull(cutoff);
        for (Reservation r : expired) {
            Product p = r.getProduct();
            int current = p.getStock() == null ? 0 : p.getStock();
            p.setStock(current + (r.getQuantity() == null ? 0 : r.getQuantity()));
            productRepository.save(p);
            reservationRepository.delete(r);
        }
    }
}
