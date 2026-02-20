package com.quantumai.customer.service;

import com.quantumai.customer.dto.InspectionCompletedCountPerDayDTO;
import com.quantumai.customer.dto.UserInspectionAnalyticsDTO;
import com.quantumai.customer.entity.Assets;
import com.quantumai.customer.entity.InspectionStepValues;
import com.quantumai.customer.entity.InspectionTemplateResult;
import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import com.quantumai.customer.repository.AssetCategoryInspectionInstanceRepository;
import com.quantumai.customer.repository.AssetsRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssetInspectionServiceImpl implements AssetInspectionService {

    @Autowired
    private AssetCategoryInspectionInstanceRepository assetCategoryInspectionInstanceRepository;

    @Autowired
    private AssetsRepository assetsRepository;
    @Override
    public List<UserInspectionAnalyticsDTO> getUserInspectionAnalytics(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        List<UserInspectionAnalyticsDTO> userInspectionAnalyticsDTOList = new ArrayList<>();
        Map<String, Long> countByActionPerformedBy =
                assetCategoryInspectionInstanceList.stream()
                        .filter(data -> data.getStatus() == InspectionInstanceStatus.COMPLETED)
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

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ─────────────────────────────────────────
            // SHEET 1 — Overview
            // ─────────────────────────────────────────
            String[] overviewHeaders = {
                    "Asset ID", "Asset Name", "Inspection ID", "Inspection Name",
                    "Date Started", "Date Completed", "Inspector", "Geo Location"
            };

            Sheet overviewSheet = workbook.createSheet("Overview");
            Row overviewHeaderRow = overviewSheet.createRow(0);
            for (int i = 0; i < overviewHeaders.length; i++) {
                overviewHeaderRow.createCell(i).setCellValue(overviewHeaders[i]);
            }

            // ─────────────────────────────────────────
            // SHEET 2 — Detailed
            // ─────────────────────────────────────────
            String[] detailedHeaders = {
                    "Asset ID", "Asset Name", "Inspection ID", "Inspection Name",
                    "Date Started", "Date Completed", "Inspector",
                    "Instruction Name", "Instruction Value", "Notes", "Geo Location"
            };

            Sheet detailedSheet = workbook.createSheet("Detailed");
            Row detailedHeaderRow = detailedSheet.createRow(0);
            for (int i = 0; i < detailedHeaders.length; i++) {
                detailedHeaderRow.createCell(i).setCellValue(detailedHeaders[i]);
            }

            int overviewRowIndex = 1;
            int detailedRowIndex = 1;

            for (AssetCategoryInspectionInstance inspection : inspections) {

                String dateStarted = "";
                String dateCompleted = "";

                if (inspection.getCreatedAt() != null) {
                    dateStarted = java.time.LocalDateTime.parse(
                            inspection.getCreatedAt().toString(),
                            java.time.format.DateTimeFormatter.ISO_DATE_TIME
                    ).toLocalDate().toString();
                }

                if (inspection.getUpdatedAt() != null) {
                    dateCompleted = java.time.LocalDateTime.parse(
                            inspection.getUpdatedAt().toString(),
                            java.time.format.DateTimeFormatter.ISO_DATE_TIME
                    ).toLocalDate().toString();
                }

                boolean hasSteps = inspection.getStepValues() != null && !inspection.getStepValues().isEmpty();

                if (hasSteps) {
                    // Overview — one row per inspection (use first step as representative, or just write once)
                    // We write one overview row per inspection instance, not per step
                    Row overviewRow = overviewSheet.createRow(overviewRowIndex++);
                    overviewRow.createCell(0).setCellValue(myAsset.getAssetId());
                    overviewRow.createCell(1).setCellValue(myAsset.getName());
                    overviewRow.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                    overviewRow.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                    overviewRow.createCell(4).setCellValue(dateStarted);
                    overviewRow.createCell(5).setCellValue(dateCompleted);
                    overviewRow.createCell(6).setCellValue(inspection.getActionPerformedBy());
                    overviewRow.createCell(7).setCellValue("");

                    // Detailed — one row per step
                    for (InspectionStepValues step : inspection.getStepValues()) {
                        Row detailedRow = detailedSheet.createRow(detailedRowIndex++);
                        detailedRow.createCell(0).setCellValue(myAsset.getAssetId());
                        detailedRow.createCell(1).setCellValue(myAsset.getName());
                        detailedRow.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                        detailedRow.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                        detailedRow.createCell(4).setCellValue(dateStarted);
                        detailedRow.createCell(5).setCellValue(dateCompleted);
                        detailedRow.createCell(6).setCellValue(inspection.getActionPerformedBy());
                        detailedRow.createCell(7).setCellValue(step.getName());
                        detailedRow.createCell(8).setCellValue(step.getValue());
                        detailedRow.createCell(9).setCellValue(inspection.getNotes());
                        detailedRow.createCell(10).setCellValue("");
                    }

                } else {
                    // Overview
                    Row overviewRow = overviewSheet.createRow(overviewRowIndex++);
                    overviewRow.createCell(0).setCellValue(inspection.getAssetId());
                    overviewRow.createCell(1).setCellValue("");
                    overviewRow.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                    overviewRow.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                    overviewRow.createCell(4).setCellValue(inspection.getCreatedAt() != null ? inspection.getCreatedAt().toString() : "");
                    overviewRow.createCell(5).setCellValue(inspection.getUpdatedAt() != null ? inspection.getUpdatedAt().toString() : "");
                    overviewRow.createCell(6).setCellValue(inspection.getActionPerformedBy());
                    overviewRow.createCell(7).setCellValue("");

                    // Detailed
                    Row detailedRow = detailedSheet.createRow(detailedRowIndex++);
                    detailedRow.createCell(0).setCellValue(inspection.getAssetId());
                    detailedRow.createCell(1).setCellValue("");
                    detailedRow.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                    detailedRow.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                    detailedRow.createCell(4).setCellValue(inspection.getCreatedAt() != null ? inspection.getCreatedAt().toString() : "");
                    detailedRow.createCell(5).setCellValue(inspection.getUpdatedAt() != null ? inspection.getUpdatedAt().toString() : "");
                    detailedRow.createCell(6).setCellValue(inspection.getActionPerformedBy());
                    detailedRow.createCell(7).setCellValue("");
                    detailedRow.createCell(8).setCellValue("");
                    detailedRow.createCell(9).setCellValue(inspection.getNotes());
                    detailedRow.createCell(10).setCellValue("");
                }
            }

            // Auto-size all columns in both sheets
            for (int i = 0; i < overviewHeaders.length; i++) {
                overviewSheet.autoSizeColumn(i);
            }
            for (int i = 0; i < detailedHeaders.length; i++) {
                detailedSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportInspectionDetailedExcel(Long companyId, String assetId) throws Exception {
        List<AssetCategoryInspectionInstance> inspections =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId)
                        .stream()
                        .filter(i -> assetId.equals(i.getAssetId()))
                        .toList();
        Optional<Assets> optionalAssets=assetsRepository.findById(assetId);

        if(optionalAssets.isEmpty()){
            throw new Exception("No Asset Found");
        }
        Assets myAsset=optionalAssets.get();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inspection Details");

            // Header
            String[] headers = {
                    "Asset ID",
                    "Asset Name",
                    "Inspection ID",
                    "Inspection Name",
                    "Date Started",
                    "Date Completed",
                    "Inspector",
                    "Instruction Name",
                    "Instruction Value",
                    "Notes",
                    "Geo Location"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;

            for (AssetCategoryInspectionInstance inspection : inspections) {

                // If stepValues exist, create row per instruction
                if (inspection.getStepValues() != null && !inspection.getStepValues().isEmpty()) {

                    for (InspectionStepValues step : inspection.getStepValues()) {

                        Row row = sheet.createRow(rowIndex++);

                        row.createCell(0).setCellValue(myAsset.getAssetId());
                        row.createCell(1).setCellValue(myAsset.getName()); // Asset Name (not available in entity)
                        row.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                        row.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                        java.time.LocalDateTime createdAt = java.time.LocalDateTime.parse(
                                inspection.getCreatedAt().toString(),
                                java.time.format.DateTimeFormatter.ISO_DATE_TIME
                        );

                        row.createCell(4).setCellValue(
                                inspection.getCreatedAt() != null ? createdAt.toLocalDate().toString() : ""
                        );
                        row.createCell(5).setCellValue(
                                inspection.getUpdatedAt() != null ?
                                        java.time.LocalDateTime.parse(
                                                inspection.getUpdatedAt().toString(),
                                                java.time.format.DateTimeFormatter.ISO_DATE_TIME
                                        ).toLocalDate().toString() : ""
                        );
                        row.createCell(6).setCellValue(inspection.getActionPerformedBy());
                        row.createCell(7).setCellValue(step.getName());
                        row.createCell(8).setCellValue(step.getValue());
                        row.createCell(9).setCellValue(inspection.getNotes());
                        row.createCell(10).setCellValue(""); // Geo Location if available elsewhere
                    }

                } else {
                    Row row = sheet.createRow(rowIndex++);

                    row.createCell(0).setCellValue(inspection.getAssetId());
                    row.createCell(1).setCellValue("");
                    row.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                    row.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                    row.createCell(4).setCellValue(
                            inspection.getCreatedAt() != null
                                    ? inspection.getCreatedAt().toString()
                                    : "");
                    row.createCell(5).setCellValue(
                            inspection.getUpdatedAt() != null
                                    ? inspection.getUpdatedAt().toString()
                                    : "");
                    row.createCell(6).setCellValue(inspection.getActionPerformedBy());
                    row.createCell(7).setCellValue("");
                    row.createCell(8).setCellValue("");
                    row.createCell(9).setCellValue(inspection.getNotes());
                    row.createCell(10).setCellValue("");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
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
        Optional<Assets> optionalAssets=assetsRepository.findById(assetId);

        if(optionalAssets.isEmpty()){
            throw new Exception("No Asset Found");
        }
        Assets myAsset=optionalAssets.get();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inspection Details");

            // Header
            String[] headers = {
                    "Asset ID",
                    "Asset Name",
                    "Inspection ID",
                    "Inspection Name",
                    "Date Started",
                    "Date Completed",
                    "Inspector",
                    "Geo Location"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;

            for (AssetCategoryInspectionInstance inspection : inspections) {

                // If stepValues exist, create row per instruction
                if (inspection.getStepValues() != null && !inspection.getStepValues().isEmpty()) {

                    for (InspectionStepValues step : inspection.getStepValues()) {

                        Row row = sheet.createRow(rowIndex++);

                        row.createCell(0).setCellValue(myAsset.getAssetId());
                        row.createCell(1).setCellValue(myAsset.getName()); // Asset Name (not available in entity)
                        row.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                        row.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                        java.time.LocalDateTime createdAt = java.time.LocalDateTime.parse(
                                inspection.getCreatedAt().toString(),
                                java.time.format.DateTimeFormatter.ISO_DATE_TIME
                        );

                        row.createCell(4).setCellValue(
                                inspection.getCreatedAt() != null ? createdAt.toLocalDate().toString() : ""
                        );
                        row.createCell(5).setCellValue(
                                inspection.getUpdatedAt() != null ?
                                        java.time.LocalDateTime.parse(
                                                inspection.getUpdatedAt().toString(),
                                                java.time.format.DateTimeFormatter.ISO_DATE_TIME
                                        ).toLocalDate().toString() : ""
                        );
                        row.createCell(6).setCellValue(inspection.getActionPerformedBy());
                        row.createCell(7).setCellValue(""); // Geo Location if available elsewhere
                    }

                } else {
                    Row row = sheet.createRow(rowIndex++);

                    row.createCell(0).setCellValue(inspection.getAssetId());
                    row.createCell(1).setCellValue("");
                    row.createCell(2).setCellValue(inspection.getAssetCategoryInspectionId());
                    row.createCell(3).setCellValue(inspection.getAssetCategoryInspectionName());
                    row.createCell(4).setCellValue(
                            inspection.getCreatedAt() != null
                                    ? inspection.getCreatedAt().toString()
                                    : "");
                    row.createCell(5).setCellValue(
                            inspection.getUpdatedAt() != null
                                    ? inspection.getUpdatedAt().toString()
                                    : "");
                    row.createCell(6).setCellValue(inspection.getActionPerformedBy());
                    row.createCell(7).setCellValue("");

                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }


}
