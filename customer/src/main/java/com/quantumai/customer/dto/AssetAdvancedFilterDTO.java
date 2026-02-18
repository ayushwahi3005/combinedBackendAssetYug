package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetAdvancedFilterDTO {
    private String assetId;
    private String name;
    private String customer;
    private String serialNumber;
    private String category;
    private String location;
    private String status;
    private String email;
    private Long companyId;
    private Integer pageNumber = 0;
    private Integer pageSize = 10;
    private String sortField;
    private String sortDirection = "DESC";
    private Map<String, String> customFields;

    public String getEffectiveSortField() {
        return (sortField == null || sortField.isEmpty()) ? "updatedAt" : sortField;
    }

    public boolean hasCustomFieldFilters() {
        return customFields != null && !customFields.isEmpty();
    }
}

