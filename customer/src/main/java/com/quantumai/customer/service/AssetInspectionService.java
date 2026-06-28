package com.quantumai.customer.service;

import com.quantumai.customer.dto.InspectionCompletedCountPerDayDTO;
import com.quantumai.customer.dto.InspectionDetailFilterDTO;
import com.quantumai.customer.dto.InspectionPerformerGroupDTO;
import com.quantumai.customer.dto.InspectionStatusCountDTO;
import com.quantumai.customer.dto.PaginatedInspectionDetailDTO;
import com.quantumai.customer.dto.UserInspectionAnalyticsDTO;
import com.quantumai.customer.entity.enums.InspectionInstanceStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AssetInspectionService {
    public List<UserInspectionAnalyticsDTO> getUserInspectionAnalytics(Long companyId, LocalDate startDate, LocalDate endDate);

    public Map<InspectionInstanceStatus,Long> getStatusDistribution(Long companyId, LocalDate startDate, LocalDate endDate);
    public Map<String,Long> getInspectionTypeCompletion(Long companyId, LocalDate startDate, LocalDate endDate);

    public Map<String, Long> getLeadInspector(Long companyId, LocalDate startDate, LocalDate endDate);

    public Map<String, Long> getAssetInspectionDetails(Long companyId);

    List<InspectionCompletedCountPerDayDTO> getInspectionCompletionPerDay(Long companyId, LocalDate startDate, LocalDate endDate);

    public byte[] exportInspectionExcel(Long companyId, String assetId) throws Exception;

    public byte[] exportInspectionDetailedExcel(Long companyId, String assetId) throws Exception;

    public byte[] exportInspectionOverviewExcel(Long companyId, String assetId) throws Exception;

    /**
     * Get inspection count grouped by status with total count for a specific company
     */
    InspectionStatusCountDTO getInspectionStatusCounts(Long companyId);

    /**
     * Get incomplete (not completed and not cancelled) inspections grouped by actionPerformedBy
     */
    InspectionPerformerGroupDTO getIncompleteInspectionsByPerformer(Long companyId);

    /**
     * Get detailed inspections with advanced filtering, pagination, and sorting
     */
    PaginatedInspectionDetailDTO getDetailedInspections(Long companyId, InspectionDetailFilterDTO filter);
}
