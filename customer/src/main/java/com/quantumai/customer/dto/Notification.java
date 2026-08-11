package com.quantumai.customer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Notification {
    private String id;
    private String userId;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime timeStamp;


  private String createdBy;
  private String lastUpdatedBy;
}
