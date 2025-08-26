package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "TrialStatus")
public class TrialStatus {
    @Id
    private String id;
    private String customerEmail;
    private Long companyId;
    private LocalDateTime trialStartDate;
    private LocalDateTime trialEndDate;
    private boolean isTrialActive;
    private boolean trialExpired;
    private boolean trialExpirationNotificationSent;
    private boolean finalWarningNotificationSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public TrialStatus() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
