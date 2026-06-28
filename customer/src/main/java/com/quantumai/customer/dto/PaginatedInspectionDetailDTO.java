package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedInspectionDetailDTO {
    private List<InspectionDetailResponseDTO> data;
    private long totalRecords;
    private int currentPage;
    private int pageSize;
    private long totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
