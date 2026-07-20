package com.quantumai.customer.service;

import com.quantumai.customer.dto.InspectionCompletedCountPerDayDTO;
import com.quantumai.customer.dto.InspectionDetailFilterDTO;
import com.quantumai.customer.dto.InspectionPerformerGroupDTO;
import com.quantumai.customer.dto.InspectionStatusCountDTO;
import com.quantumai.customer.dto.PaginatedInspectionDetailDTO;
import com.quantumai.customer.dto.UserInspectionAnalyticsDTO;
import com.quantumai.customer.entity.Assets;
import com.quantumai.customer.entity.InspectionStepValues;
import com.quantumai.customer.entity.InspectionTemplateResult;
import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import com.quantumai.customer.repository.AssetCategoryInspectionInstanceRepository;
import com.quantumai.customer.repository.AssetsRepository;
import com.quantumai.customer.util.LocationHierarchyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssetInspectionServiceImpl implements AssetInspectionService {

    @Autowired
    private AssetCategoryInspectionInstanceRepository assetCategoryInspectionInstanceRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    @Autowired
    private LocationHierarchyUtil locationHierarchyUtil;
    @Override
    public List<UserInspectionAnalyticsDTO> getUserInspectionAnalytics(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        List<UserInspectionAnalyticsDTO> userInspectionAnalyticsDTOList = new ArrayList<>();
        log.info("getUserInspectionAnalytics Start Date: {}, End Date: {}", startDate, endDate);
        Map<String, Long> countByActionPerformedBy =
                assetCategoryInspectionInstanceList.stream()
                        .filter(data -> data.getStatus() == InspectionInstanceStatus.COMPLETED)
                        .filter(data -> {
                            LocalDateTime updatedAt = data.getUpdatedAt();
                            return updatedAt != null &&
                                    !updatedAt.toLocalDate().isBefore(startDate) &&
                                    !updatedAt.toLocalDate().isAfter(endDate);
                        })
                        .collect(Collectors.groupingBy(
                                AssetCategoryInspectionInstance::getActionPerformedBy,
                                Collectors.counting()
                        ))
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));

        countByActionPerformedBy.forEach((userName, count) -> {
            UserInspectionAnalyticsDTO userInspectionAnalyticsDTO = new UserInspectionAnalyticsDTO();
            userInspectionAnalyticsDTO.setUserName(userName);
            userInspectionAnalyticsDTO.setTotalCompletedInspections(count);
            // In a real scenario, you would fetch the userName from a User service or repository
//            userInspectionAnalyticsDTO.setUserName("User_" + userId); // Placeholder for user name
            userInspectionAnalyticsDTOList.add(userInspectionAnalyticsDTO);
        });
        return userInspectionAnalyticsDTOList;
    }

    @Override
    public Map<InspectionInstanceStatus, Long> getStatusDistribution(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);

        Map<InspectionInstanceStatus, Long> countByStatus =
                assetCategoryInspectionInstanceList.stream()
                        .filter(data -> {
                            LocalDateTime updatedAt = data.getUpdatedAt();
                            return updatedAt != null &&
                                    !updatedAt.toLocalDate().isBefore(startDate) &&
                                    !updatedAt.toLocalDate().isAfter(endDate);
                        })
                        .collect(Collectors.groupingBy(
                                AssetCategoryInspectionInstance::getStatus,
                                Collectors.counting()
                        ));
        return countByStatus;

    }

    @Override
    public Map<String, Long> getInspectionTypeCompletion(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        Map<String, Long> completedInspectionTypeCount =
                assetCategoryInspectionInstanceList.stream()
                        .filter(i -> i.getStatus() == InspectionInstanceStatus.COMPLETED)
                        .flatMap(i -> Optional.ofNullable(i.getInspectionTemplates())
                                .orElse(Collections.emptyList())
                                .stream())
                        .collect(Collectors.groupingBy(
                                InspectionTemplateResult::getInspectionName,
                                Collectors.counting()
                        ));


        return completedInspectionTypeCount;
    }

    @Override
    public Map<String, Long> getLeadInspector(Long companyId,
                                              LocalDate startDate,
                                              LocalDate endDate) {

        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);

        return assetCategoryInspectionInstanceList.stream()
                .filter(i -> i.getStatus() == InspectionInstanceStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        AssetCategoryInspectionInstance::getActionPerformedBy,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByKey()) // max count
                .map(maxEntry ->
                        maxEntry.getValue().stream()
                                .sorted(Map.Entry.comparingByKey()) // alphabetical
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                ))
                )
                .orElse(new LinkedHashMap<>());
    }

    @Override
    public Map<String, Long> getAssetInspectionDetails(Long companyId) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        long totalAsset=assetCategoryInspectionInstanceList.stream()
                .map(AssetCategoryInspectionInstance::getAssetId).distinct().count();
        long completedInspections=
        assetCategoryInspectionInstanceList.stream().filter(i->i.getStatus()==InspectionInstanceStatus.COMPLETED).count();

        long totalInspections=assetCategoryInspectionInstanceList.size();

        Map<String, Long> inspectionDetails = new HashMap<>();
        inspectionDetails.put("totalAssetsInspected", totalAsset);
        inspectionDetails.put("completedInspections", completedInspections);
        inspectionDetails.put("totalInspections", totalInspections);
        return inspectionDetails;


    }

    @Override
    public List<InspectionCompletedCountPerDayDTO> getInspectionCompletionPerDay(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        log.info("strat date:{} end date:{}",startDate,endDate);
        try {
            Thread.sleep(3000); // 3000 ms = 3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<AssetCategoryInspectionInstance> instances =
                assetCategoryInspectionInstanceRepository
                        .findByCompanyIdAndUpdatedAtBetween(
                                companyId,
                                startDate.atStartOfDay(),
                                endDate.atTime(23, 59, 59)
                        );

        log.info("AssetCategoryInspectionInstance {} {} ",instances.size(),instances);



        Map<LocalDate, Long> inspectionsPerDay =
                instances.stream()
                        .filter(i -> i.getStatus() == InspectionInstanceStatus.COMPLETED)
                        .collect(Collectors.groupingBy(
                                i -> i.getUpdatedAt().toLocalDate(),
                                Collectors.counting()
                        ));

        long completedCount = instances.stream()
                .filter(i -> i.getStatus() == InspectionInstanceStatus.COMPLETED)
                .count();

        System.out.println("Total instances: " + instances.size());
        System.out.println("Completed instances: " + completedCount);

        List<InspectionCompletedCountPerDayDTO> result =
                inspectionsPerDay.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> {
                            InspectionCompletedCountPerDayDTO inspectionCompletedCountPerDayDTO = new InspectionCompletedCountPerDayDTO();
                            inspectionCompletedCountPerDayDTO.setDate(e.getKey());
                            inspectionCompletedCountPerDayDTO.setCount(e.getValue());

                            return inspectionCompletedCountPerDayDTO;
                        })
                        .collect(Collectors.toList());

        log.info("Inspections Per Day: {}", result);
        return result;
    }

    @Override
    public byte[] exportInspectionExcel(Long companyId, String assetId) throws Exception {
        return exportInspectionDetailedExcel(companyId, assetId);
    }

    @Override
    public byte[] exportInspectionDetailedExcel(Long companyId, String assetId) throws Exception {
        List<AssetCategoryInspectionInstance> inspections =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId)
                        .stream()
                        .filter(i -> assetId.equals(i.getAssetId()))
                        .toList();
        Optional<Assets> optionalAssets = assetsRepository.findById(assetId);

        if (optionalAssets.isEmpty()) {
            throw new Exception("No Asset Found");
        }
        Assets myAsset = optionalAssets.get();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Map<String, AssetCategoryInspectionInstance> uniqueInspections = new LinkedHashMap<>();
        for (AssetCategoryInspectionInstance inspection : inspections) {
            String dedupeKey = inspection.getAssetCategoryInspectionInstanceId() != null
                    ? String.valueOf(inspection.getAssetCategoryInspectionInstanceId())
                    : inspection.getId();
            if (dedupeKey != null) {
                uniqueInspections.putIfAbsent(dedupeKey, inspection);
            }
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String[] overviewHeaders = {
                    "Asset ID",
                    "Asset Name",
                    "Asset Location",
                    "Customer",
                    "Asset Category",
                    "Asset Serial Number",
                    "Inspection ID",
                    "Inspection Name",
                    "Inspection Status",
                    "Created Date",
                    "Due Date",
                    "Date Completed",
                    "Performed By",
                    "Last Modified Date",
                    "Last Modified User",
                    "Notes"
            };

            Sheet overviewSheet = workbook.createSheet("Inspection Overview");
            Row overviewHeaderRow = overviewSheet.createRow(0);
            for (int i = 0; i < overviewHeaders.length; i++) {
                overviewHeaderRow.createCell(i).setCellValue(overviewHeaders[i]);
            }

            int overviewRowIndex = 1;
            for (AssetCategoryInspectionInstance inspection : uniqueInspections.values()) {
                Row row = overviewSheet.createRow(overviewRowIndex++);
                populateOverviewExportRow(row, myAsset, inspection, dateTimeFormatter);
            }
            for (int i = 0; i < overviewHeaders.length; i++) {
                overviewSheet.autoSizeColumn(i);
            }

            String[] detailedHeaders = {
                    "Asset ID",
                    "Asset Name",
                    "Inspection ID",
                    "Inspection Name",
                    "Inspection Status",
                    "Created Date",
                    "Date Completed",
                    "Performed By",
                    "Instruction Name",
                    "Instruction Value",
                    "Notes",
                    "Last Modified Date",
                    "Last Modified User"
            };

            Sheet detailedSheet = workbook.createSheet("Inspection Details");
            Row detailedHeaderRow = detailedSheet.createRow(0);
            for (int i = 0; i < detailedHeaders.length; i++) {
                detailedHeaderRow.createCell(i).setCellValue(detailedHeaders[i]);
            }

            int detailedRowIndex = 1;
            for (AssetCategoryInspectionInstance inspection : inspections) {
                if (inspection.getStepValues() != null && !inspection.getStepValues().isEmpty()) {
                    for (InspectionStepValues step : inspection.getStepValues()) {
                        Row row = detailedSheet.createRow(detailedRowIndex++);
                        populateDetailedExportRow(row, myAsset, inspection, step, dateTimeFormatter);
                    }
                } else {
                    Row row = detailedSheet.createRow(detailedRowIndex++);
                    populateDetailedExportRow(row, myAsset, inspection, null, dateTimeFormatter);
                }
            }
            for (int i = 0; i < detailedHeaders.length; i++) {
                detailedSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportInspectionOverviewExcel(Long companyId, String assetId) throws Exception {
        List<AssetCategoryInspectionInstance> inspections =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId)
                        .stream()
                        .filter(i -> assetId.equals(i.getAssetId()))
                        .toList();
        Optional<Assets> optionalAssets = assetsRepository.findById(assetId);

        if (optionalAssets.isEmpty()) {
            throw new Exception("No Asset Found");
        }
        Assets myAsset = optionalAssets.get();

        Map<String, AssetCategoryInspectionInstance> uniqueInspections = new LinkedHashMap<>();
        for (AssetCategoryInspectionInstance inspection : inspections) {
            String dedupeKey = inspection.getAssetCategoryInspectionInstanceId() != null
                    ? String.valueOf(inspection.getAssetCategoryInspectionInstanceId())
                    : inspection.getId();
            if (dedupeKey != null) {
                uniqueInspections.putIfAbsent(dedupeKey, inspection);
            }
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inspection Overview");

            String[] headers = {
                    "Asset ID",
                    "Asset Name",
                    "Asset Location",
                    "Customer",
                    "Asset Category",
                    "Asset Serial Number",
                    "Inspection ID",
                    "Inspection Name",
                    "Inspection Status",
                    "Created Date",
                    "Due Date",
                    "Date Completed",
                    "Performed By",
                    "Last Modified Date",
                    "Last Modified User",
                    "Notes"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;

            for (AssetCategoryInspectionInstance inspection : uniqueInspections.values()) {
                Row row = sheet.createRow(rowIndex++);
                populateOverviewExportRow(row, myAsset, inspection, dateTimeFormatter);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void populateOverviewExportRow(
            Row row,
            Assets asset,
            AssetCategoryInspectionInstance inspection,
            DateTimeFormatter dateTimeFormatter) {
        row.createCell(0).setCellValue(asset.getAssetId() != null ? asset.getAssetId() : 0);
        row.createCell(1).setCellValue(nullToEmpty(asset.getName()));
        row.createCell(2).setCellValue(locationHierarchyUtil.resolveAssetLocationName(asset));
        row.createCell(3).setCellValue(nullToEmpty(asset.getCustomer()));
        row.createCell(4).setCellValue(nullToEmpty(asset.getCategory()));
        row.createCell(5).setCellValue(nullToEmpty(asset.getSerialNumber()));
        row.createCell(6).setCellValue(
                inspection.getAssetCategoryInspectionInstanceId() != null
                        ? inspection.getAssetCategoryInspectionInstanceId().doubleValue()
                        : 0);
        row.createCell(7).setCellValue(nullToEmpty(inspection.getAssetCategoryInspectionName()));
        row.createCell(8).setCellValue(
                inspection.getStatus() != null ? inspection.getStatus().toString() : "");
        row.createCell(9).setCellValue(formatInspectionDateTime(inspection.getCreatedAt(), dateTimeFormatter));
        row.createCell(10).setCellValue(resolveDueDate(inspection));
        row.createCell(11).setCellValue(resolveDateCompleted(inspection, dateTimeFormatter));
        row.createCell(12).setCellValue(nullToEmpty(inspection.getActionPerformedBy()));
        row.createCell(13).setCellValue(formatInspectionDateTime(inspection.getUpdatedAt(), dateTimeFormatter));
        row.createCell(14).setCellValue(nullToEmpty(inspection.getActionPerformedBy()));
        row.createCell(15).setCellValue(nullToEmpty(inspection.getNotes()));
    }

    private void populateDetailedExportRow(
            Row row,
            Assets asset,
            AssetCategoryInspectionInstance inspection,
            InspectionStepValues step,
            DateTimeFormatter dateTimeFormatter) {
        row.createCell(0).setCellValue(asset.getAssetId() != null ? asset.getAssetId() : 0);
        row.createCell(1).setCellValue(nullToEmpty(asset.getName()));
        row.createCell(2).setCellValue(
                inspection.getAssetCategoryInspectionInstanceId() != null
                        ? inspection.getAssetCategoryInspectionInstanceId().doubleValue()
                        : 0);
        row.createCell(3).setCellValue(nullToEmpty(inspection.getAssetCategoryInspectionName()));
        row.createCell(4).setCellValue(
                inspection.getStatus() != null ? inspection.getStatus().toString() : "");
        row.createCell(5).setCellValue(formatInspectionDateTime(inspection.getCreatedAt(), dateTimeFormatter));
        row.createCell(6).setCellValue(resolveDateCompleted(inspection, dateTimeFormatter));
        row.createCell(7).setCellValue(nullToEmpty(inspection.getActionPerformedBy()));
        row.createCell(8).setCellValue(step != null ? nullToEmpty(step.getName()) : "");
        row.createCell(9).setCellValue(step != null ? nullToEmpty(step.getValue()) : "");
        row.createCell(10).setCellValue(nullToEmpty(inspection.getNotes()));
        row.createCell(11).setCellValue(formatInspectionDateTime(inspection.getUpdatedAt(), dateTimeFormatter));
        row.createCell(12).setCellValue(nullToEmpty(inspection.getActionPerformedBy()));
    }

    private String resolveDueDate(AssetCategoryInspectionInstance inspection) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (inspection.getInspectionDueDate() != null) {
            return inspection.getInspectionDueDate().format(dateFormatter);
        }
        if (inspection.getDueDate() != null) {
            return inspection.getDueDate().format(dateFormatter);
        }
        return "";
    }

    private String resolveDateCompleted(
            AssetCategoryInspectionInstance inspection, DateTimeFormatter dateTimeFormatter) {
        if (inspection.getStatus() == InspectionInstanceStatus.COMPLETED
                && inspection.getUpdatedAt() != null) {
            return formatInspectionDateTime(inspection.getUpdatedAt(), dateTimeFormatter);
        }
        return "";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String formatInspectionDateTime(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime != null ? dateTime.format(formatter) : "";
    }

    @Override
    public com.quantumai.customer.dto.InspectionStatusCountDTO getInspectionStatusCounts(Long companyId) {
        log.info("Getting inspection status counts for companyId: {}", companyId);
        return assetCategoryInspectionInstanceRepository.getStatusCountByCompanyId(companyId);
    }

    @Override
    public com.quantumai.customer.dto.InspectionPerformerGroupDTO getIncompleteInspectionsByPerformer(Long companyId) {
        log.info("Getting incomplete inspections grouped by performer for companyId: {}", companyId);
        List<Map<String, Object>> performerCounts = assetCategoryInspectionInstanceRepository.getIncompleteNotCancelledByPerformer(companyId);
        return new com.quantumai.customer.dto.InspectionPerformerGroupDTO(performerCounts);
    }

    @Override
    public com.quantumai.customer.dto.PaginatedInspectionDetailDTO getDetailedInspections(Long companyId, com.quantumai.customer.dto.InspectionDetailFilterDTO filter) {
        log.info("Getting detailed inspections for companyId: {} with filters", companyId);
        return assetCategoryInspectionInstanceRepository.getDetailedInspectionsWithFiltering(companyId, filter);
    }


}
