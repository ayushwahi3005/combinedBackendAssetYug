package com.quantumai.customer.dto;


import lombok.Data;

import java.time.LocalDateTime;


@Data
public class UserNotificationDTO {

    private String id;
    private String userId;
    private String notificationId;
    private Long companyId;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime deliveredAt;
    private Notification notification;

  private String createdBy;
  private String lastUpdatedBy;
}
