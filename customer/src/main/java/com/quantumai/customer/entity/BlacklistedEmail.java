package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Stores emails of users whose accounts were deleted after trial expiration
 * without subscribing. These users are not eligible for another free trial.
 */
@Data
@Document(collection = "BlacklistedEmails")
public class BlacklistedEmail {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private Long previousCompanyId;
    private String reason;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    public BlacklistedEmail() {
        this.createdAt = LocalDateTime.now();
    }

    public BlacklistedEmail(String email, Long previousCompanyId, String reason) {
        this.email = email;
        this.previousCompanyId = previousCompanyId;
        this.reason = reason;
        this.deletedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
