package com.quantumai.customer.entity;

import com.quantumai.customer.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Data
@Document
@NoArgsConstructor
public class UserNotification {

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
    private String createdBy;
    private String lastUpdatedBy;

    public UserNotification(String id, String userId, String notificationId, Long companyId,
                            boolean isRead, LocalDateTime readAt, LocalDateTime deliveredAt,
                            Notification notification) {
        this.id = id;
        this.userId = userId;
        this.notificationId = notificationId;
        this.companyId = companyId;
        this.isRead = isRead;
        this.readAt = readAt;
        this.deliveredAt = deliveredAt;
        this.notification = notification;
    }
}
