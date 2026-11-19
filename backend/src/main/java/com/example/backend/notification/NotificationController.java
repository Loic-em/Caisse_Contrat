package com.example.backend.notification;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Notification> list(@RequestParam String recipientEmail) {
        return service.list(recipientEmail);
    }

    @GetMapping("/unread-count")
    public long unreadCount(@RequestParam String recipientEmail) {
        return service.unreadCount(recipientEmail);
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable UUID id) {
        service.markRead(id);
    }
}
