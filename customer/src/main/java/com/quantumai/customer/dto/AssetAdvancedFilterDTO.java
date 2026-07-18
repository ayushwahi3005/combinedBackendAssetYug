package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;
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
  /** Global search applied after other filters (matches standard and custom field values). */
    @JsonAlias({"searchData", "searchTerm"})
    private String search;
  /** Filter by check-in/out status: "Checked In" or "Checked Out". */
    @JsonAlias({"checkInOutStatus", "checkedInOutStatus"})
    private String checkedInOut;
    private Map<String, String> customFields;

    public String getEffectiveSortField() {
        return (sortField == null || sortField.isEmpty()) ? "updatedAt" : sortField;
    }

    public boolean hasCustomFieldFilters() {
        if (customFields == null || customFields.isEmpty()) {
            return false;
        }
        return customFields.entrySet().stream()
            .anyMatch(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty());
    }
}

