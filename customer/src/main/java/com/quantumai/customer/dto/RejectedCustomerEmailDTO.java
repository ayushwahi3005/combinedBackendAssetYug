package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectedCustomerEmailDTO {

    private String id;
    private String email;
    private Long companyId;
    private String reason;
    private LocalDateTime rejectionDate;
    private LocalDateTime accountDeleteDate;
    private boolean isAccountDeleted;
    private LocalDateTime originalTrialEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

