package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document
public class ActiveSession {

    @Id
    private String sessionId;
    private String userId;
    private String deviceId;
    private String userAgent;
    private LocalDateTime lastActivityTime;
}
