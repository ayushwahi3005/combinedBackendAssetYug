package com.quantumai.customer.repository;

import com.quantumai.customer.dto.InspectionDetailFilterDTO;
import com.quantumai.customer.dto.InspectionStatusCountDTO;
import com.quantumai.customer.dto.PaginatedInspectionDetailDTO;

import java.util.Map;
import java.util.List;

public interface AssetCategoryInspectionInstanceRepositoryCustom {

    /**
     * Get inspection count grouped by status with total count for a specific company
     */
    InspectionStatusCountDTO getStatusCountByCompanyId(Long companyId);

    /**
     * Get incomplete (not completed and not cancelled) inspections grouped by actionPerformedBy
     */
    List<Map<String, Object>> getIncompleteNotCancelledByPerformer(Long companyId);

    /**
     * Get detailed inspections with advanced filtering, pagination, and sorting
     */
    PaginatedInspectionDetailDTO getDetailedInspectionsWithFiltering(Long companyId, InspectionDetailFilterDTO filter);
}
