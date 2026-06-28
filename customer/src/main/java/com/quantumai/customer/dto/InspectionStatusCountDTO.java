package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionStatusCountDTO {
    private List<Map<String, Object>> statusCounts;
    private long totalInspections;
}
