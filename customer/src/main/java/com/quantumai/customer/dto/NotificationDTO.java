package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String userId;
    private String message;
    private Long companyId;
    private String type;
    private boolean read;
    private LocalDateTime timeStamp;

}
