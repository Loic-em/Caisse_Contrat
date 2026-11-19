package com.example.backend.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private boolean readFlag = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String v) { recipientEmail = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { message = v; }
    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean v) { readFlag = v; }
    @Column
    private String relatedRequestId;

    public String getRelatedRequestId() { return relatedRequestId; }
    public void setRelatedRequestId(String v) { relatedRequestId = v; }
}
