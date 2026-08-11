package com.quantumai.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@NoArgsConstructor
public class Notification {

    @Id
    private String id;
    private String title;
    private String message;
    private NotificationType notificationType; // GLOBAL, COMPANY
    private String alertType;
    private Long companyId; // null for global
    private LocalDateTime createdAt;
    private LocalDateTime expiryAt;
    private String createdBy;
    private String lastUpdatedBy;

    public Notification(String id, String title, String message, NotificationType notificationType,
                        String alertType, Long companyId, LocalDateTime createdAt, LocalDateTime expiryAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.alertType = alertType;
        this.companyId = companyId;
        this.createdAt = createdAt;
        this.expiryAt = expiryAt;
    }
}
