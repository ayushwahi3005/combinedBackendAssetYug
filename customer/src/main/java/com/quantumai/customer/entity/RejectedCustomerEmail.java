package com.quantumai.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "RejectedCustomerEmails")
public class RejectedCustomerEmail {

    @Id
    private String id;

    private String email;

    private Long companyId;

    /**
     * Reason for rejection - "ACCOUNT_DELETED", "NO_SUBSCRIPTION"
     */
    private String reason;

    /**
     * Date when the customer was rejected
     */
    private LocalDateTime rejectionDate;

    /**
     * If ACCOUNT_DELETED - stores the account deletion date
     * If NO_SUBSCRIPTION - stores the date account should be deleted if no subscription
     */
    private LocalDateTime accountDeleteDate;

    /**
     * Flag to indicate if account has been deleted from the system
     */
    private boolean isAccountDeleted;

    /**
     * Original trial end date (for reference)
     */
    private LocalDateTime originalTrialEndDate;

    /**
     * Timestamp when this record was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of last update
     */
    private LocalDateTime updatedAt;

    public RejectedCustomerEmail(String email, Long companyId, String reason) {
        this.email = email;
        this.companyId = companyId;
        this.reason = reason;
        this.rejectionDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isAccountDeleted = false;
    }
}

