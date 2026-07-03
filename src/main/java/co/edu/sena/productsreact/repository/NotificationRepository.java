package co.edu.sena.productsreact.repository;

import co.edu.sena.productsreact.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByAudienceOrderByCreatedAtDesc(String audience);

    long countByAudienceAndReadFalse(String audience);

    List<Notification> findByAudienceAndReadFalse(String audience);
}
