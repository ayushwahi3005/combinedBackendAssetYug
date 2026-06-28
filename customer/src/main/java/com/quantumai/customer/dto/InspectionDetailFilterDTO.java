package com.quantumai.customer.dto;

import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDetailFilterDTO {
    private String customerId;
    private String customerCategory;
    private String assetId;
    private String assetName;
    private String assetCustomer;
    private String serialNumber;
    private String assetCategory;
    private String assetLocation;
    private String inspectionName;
    private InspectionInstanceStatus status;
    private LocalDate createdDateFrom;
    private LocalDate createdDateTo;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private String performedBy;
    private int pageNumber = 0;
    private int pageSize = 10;
    private String sortField = "createdAt";
    private String sortDirection = "DESC";
}
