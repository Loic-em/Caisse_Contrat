package com.example.backend.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification create(String recipientEmail, String title, String message) {
        Notification n = new Notification();
        n.setRecipientEmail(recipientEmail);
        n.setTitle(title);
        n.setMessage(message);
        return repository.save(n);
    }

    public List<Notification> list(String recipientEmail) {
        return repository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail);
    }

    public long unreadCount(String recipientEmail) {
        return repository.countByRecipientEmailAndReadFlagFalse(recipientEmail);
    }

    @Transactional
    public void markRead(java.util.UUID id) {
        repository.findById(id).ifPresent(n -> { n.setReadFlag(true); repository.save(n); });
    }
}
