package com.quantumai.customer.dto;

import com.quantumai.customer.entity.UserNotification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ✅ DTO for paginated notification response
 * Includes notifications and pagination metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedNotificationDTO {
    private List<UserNotification> notifications;
    private int pageNumber;
    private int pageSize;
    private long totalCount;
    private int totalPages;
    private boolean hasMore;
}
