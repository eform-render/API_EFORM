package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.Notification;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static co.edu.sena.productsreact.util.AppClock.nowBogota;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String ADMIN_AUDIENCE = "ADMIN";

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyAdmins(String type, String title, String message, Long relatedId) {
        notificationRepository.save(new Notification(ADMIN_AUDIENCE, type, title, message, relatedId, nowBogota()));
    }

    @Transactional
    public void notifyClient(String email, String type, String title, String message, Long relatedId) {
        notificationRepository.save(new Notification(email, type, title, message, relatedId, nowBogota()));
    }

    @Transactional(readOnly = true)
    public List<Notification> getForAudience(String audience) {
        return notificationRepository.findTop50ByAudienceOrderByCreatedAtDesc(audience);
    }

    @Transactional(readOnly = true)
    public long countUnread(String audience) {
        return notificationRepository.countByAudienceAndReadFalse(audience);
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String audience) {
        List<Notification> unread = notificationRepository.findByAudienceAndReadFalse(audience);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
