package com.quantumai.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Data
public class UserNotificationDTO {

    @Id
    private String id;
    private String userId;
    private String notificationId;
    private Long companyId;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime deliveredAt;

    @DBRef
    private Notification notification;
}
