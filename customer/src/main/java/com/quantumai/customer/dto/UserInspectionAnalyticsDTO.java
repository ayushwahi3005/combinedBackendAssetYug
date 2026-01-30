package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class UserInspectionAnalyticsDTO {
    private String userId;
    private String userName;
    private Long totalCompletedInspections;

}
