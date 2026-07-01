package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Reservation;
import co.edu.sena.productsreact.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByProductOrderByCreatedAtAsc(Product product);

    List<Reservation> findByCreatedAtBefore(LocalDateTime time);

    List<Reservation> findByPaymentId(Long paymentId);

    List<Reservation> findByReservedByAndCreatedAtAfter(String reservedBy, LocalDateTime createdAt);
}
