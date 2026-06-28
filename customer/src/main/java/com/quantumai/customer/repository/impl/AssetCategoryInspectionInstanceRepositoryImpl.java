package com.quantumai.customer.repository.impl;

import com.quantumai.customer.dto.InspectionDetailFilterDTO;
import com.quantumai.customer.dto.InspectionDetailResponseDTO;
import com.quantumai.customer.dto.InspectionStatusCountDTO;
import com.quantumai.customer.dto.PaginatedInspectionDetailDTO;
import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import com.quantumai.customer.entity.Assets;
import com.quantumai.customer.entity.CompanyCustomer;
import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import com.quantumai.customer.repository.AssetCategoryInspectionInstanceRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class AssetCategoryInspectionInstanceRepositoryImpl implements AssetCategoryInspectionInstanceRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public InspectionStatusCountDTO getStatusCountByCompanyId(Long companyId) {
        log.info("Fetching inspection status counts for companyId: {}", companyId);

        try {
            // Get all inspection instances for the company
            Query query = new Query(Criteria.where("companyId").is(companyId));
            List<AssetCategoryInspectionInstance> inspections = mongoTemplate.find(query, AssetCategoryInspectionInstance.class);

            // Group by status
            Map<String, Long> statusCountMap = inspections.stream()
                    .collect(Collectors.groupingBy(
                            inspection -> inspection.getStatus() != null ? inspection.getStatus().name() : "UNKNOWN",
                            Collectors.counting()
                    ));

            // Convert to list of maps with status and count
            List<Map<String, Object>> statusCounts = statusCountMap.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("status", entry.getKey());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .collect(Collectors.toList());

            long totalInspections = inspections.size();

            InspectionStatusCountDTO result = new InspectionStatusCountDTO();
            result.setStatusCounts(statusCounts);
            result.setTotalInspections(totalInspections);

            log.info("Status counts: {}, Total inspections: {}", statusCountMap, totalInspections);
            return result;

        } catch (Exception e) {
            log.error("Error fetching inspection status counts for companyId: {}", companyId, e);
            return new InspectionStatusCountDTO(new ArrayList<>(), 0);
        }
    }

    @Override
    public List<Map<String, Object>> getIncompleteNotCancelledByPerformer(Long companyId) {
        log.info("Fetching incomplete inspections grouped by performer for companyId: {}", companyId);

        try {
            // Query for inspections that are NOT completed and NOT cancelled
            Criteria criteria = new Criteria();
            criteria.and("companyId").is(companyId)
                    .and("status").nin(
                            InspectionInstanceStatus.COMPLETED,
                            InspectionInstanceStatus.CANCELLED
                    );

            Query query = new Query(criteria);
            List<AssetCategoryInspectionInstance> inspections = mongoTemplate.find(query, AssetCategoryInspectionInstance.class);

            // Group by actionPerformedBy
            Map<String, Long> performerCountMap = inspections.stream()
                    .filter(inspection -> inspection.getActionPerformedBy() != null && !inspection.getActionPerformedBy().isEmpty())
                    .collect(Collectors.groupingBy(
                            AssetCategoryInspectionInstance::getActionPerformedBy,
                            Collectors.counting()
                    ));

            // Convert to list of maps
            List<Map<String, Object>> performerCounts = performerCountMap.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("performedBy", entry.getKey());
                        map.put("count", entry.getValue());
                        return map;
                    })
                    .sorted((m1, m2) -> Long.compare((Long) m2.get("count"), (Long) m1.get("count")))
                    .collect(Collectors.toList());

            log.info("Performer counts: {}", performerCountMap);
            return performerCounts;

        } catch (Exception e) {
            log.error("Error fetching incomplete inspections grouped by performer for companyId: {}", companyId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public PaginatedInspectionDetailDTO getDetailedInspectionsWithFiltering(Long companyId, InspectionDetailFilterDTO filter) {
        log.info("Fetching detailed inspections with filtering for companyId: {}", companyId);
        log.info("Fetching detailed inspections with filtering for InspectionDetailFilterDTO: {}", filter.toString());

        try {
            // Build criteria
            Criteria criteria = buildFilterCriteria(companyId, filter);

            // Count total records matching filter
            long totalCount = mongoTemplate.count(new Query(criteria), AssetCategoryInspectionInstance.class);

            // Fetch paginated and sorted results
            Query query = new Query(criteria);

            // Apply sorting
            String sortField = filter.getSortField() != null && !filter.getSortField().isEmpty()
                    ? filter.getSortField()
                    : "createdAt";
            Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            query.with(Sort.by(direction, sortField));

            // Apply pagination
            int pageNumber = Math.max(filter.getPageNumber(), 0);
            int pageSize = filter.getPageSize() > 0 ? filter.getPageSize() : 10;
            query.skip((long) pageNumber * pageSize).limit(pageSize);

            List<AssetCategoryInspectionInstance> inspections = mongoTemplate.find(query, AssetCategoryInspectionInstance.class);

            // Enrich with asset details
            List<InspectionDetailResponseDTO> detailedResponses = enrichInspectionDetails(inspections);

            // Build paginated response
            PaginatedInspectionDetailDTO response = new PaginatedInspectionDetailDTO();
            response.setData(detailedResponses);
            response.setTotalRecords(totalCount);
            response.setCurrentPage(pageNumber);
            response.setPageSize(pageSize);
            response.setTotalPages((totalCount + pageSize - 1) / pageSize);
            response.setHasNext((long) (pageNumber + 1) * pageSize < totalCount);
            response.setHasPrevious(pageNumber > 0);

            log.info("Fetched {} detailed inspections out of {} total", detailedResponses.size(), totalCount);
            return response;

        } catch (Exception e) {
            log.error("Error fetching detailed inspections with filtering for companyId: {}", companyId, e);
            PaginatedInspectionDetailDTO emptyResponse = new PaginatedInspectionDetailDTO();
            emptyResponse.setData(new ArrayList<>());
            emptyResponse.setTotalRecords(0);
            return emptyResponse;
        }
    }

    /**
     * Build filter criteria based on the provided filter DTO
     */
    private Criteria buildFilterCriteria(Long companyId, InspectionDetailFilterDTO filter) {
        Criteria criteria = Criteria.where("companyId").is(companyId);

        List<String> assetMongoIds = resolveAssetMongoIds(companyId, filter);
        if (assetMongoIds != null) {
            if (assetMongoIds.isEmpty()) {
                criteria.and("assetId").is("__no_matching_assets__");
            } else {
                criteria.and("assetId").in(assetMongoIds);
            }
        }

        // Filter by inspectionName (case-insensitive)
        if (filter.getInspectionName() != null && !filter.getInspectionName().isEmpty()) {
            criteria.and("assetCategoryInspectionName").regex(filter.getInspectionName(), "i");
        }

        // Filter by status
        if (filter.getStatus() != null) {
            criteria.and("status").is(filter.getStatus());
        }

        // Filter by performedBy
        if (filter.getPerformedBy() != null && !filter.getPerformedBy().isEmpty()) {
            criteria.and("actionPerformedBy").is(filter.getPerformedBy());
        }

        // Filter by created date range
        if (filter.getCreatedDateFrom() != null || filter.getCreatedDateTo() != null) {
            if (filter.getCreatedDateFrom() != null && filter.getCreatedDateTo() != null) {
                LocalDateTime fromDateTime = filter.getCreatedDateFrom().atStartOfDay();
                LocalDateTime toDateTime = filter.getCreatedDateTo().atTime(LocalTime.MAX);
                criteria.and("createdAt").gte(fromDateTime).lte(toDateTime);
            } else if (filter.getCreatedDateFrom() != null) {
                LocalDateTime fromDateTime = filter.getCreatedDateFrom().atStartOfDay();
                criteria.and("createdAt").gte(fromDateTime);
            } else if (filter.getCreatedDateTo() != null) {
                LocalDateTime toDateTime = filter.getCreatedDateTo().atTime(LocalTime.MAX);
                criteria.and("createdAt").lte(toDateTime);
            }
        }

        // Filter by inspection due date range
        if (filter.getDueDateFrom() != null || filter.getDueDateTo() != null) {
            if (filter.getDueDateFrom() != null && filter.getDueDateTo() != null) {
                criteria.and("inspectionDueDate").gte(filter.getDueDateFrom()).lte(filter.getDueDateTo());
            } else if (filter.getDueDateFrom() != null) {
                criteria.and("inspectionDueDate").gte(filter.getDueDateFrom());
            } else {
                criteria.and("inspectionDueDate").lte(filter.getDueDateTo());
            }
        }

        return criteria;
    }

    /**
     * Resolve MongoDB asset _id values matching asset-related filters.
     * Returns null when no asset-based filter is applied.
     */
    private List<String> resolveAssetMongoIds(Long companyId, InspectionDetailFilterDTO filter) {
        boolean hasAssetFilter = hasText(filter.getAssetId())
                || hasText(filter.getAssetName())
                || hasText(filter.getAssetCustomer())
                || hasText(filter.getSerialNumber())
                || hasText(filter.getAssetCategory())
                || hasText(filter.getAssetLocation())
                || hasText(filter.getCustomerId())
                || hasText(filter.getCustomerCategory());

        if (!hasAssetFilter) {
            return null;
        }

        Query assetQuery = new Query(Criteria.where("companyId").is(companyId));

        if (hasText(filter.getAssetId())) {
            try {
                assetQuery.addCriteria(Criteria.where("assetId").is(Integer.parseInt(filter.getAssetId().trim())));
            } catch (NumberFormatException ex) {
                assetQuery.addCriteria(Criteria.where("_id").is(filter.getAssetId().trim()));
            }
        }
        if (hasText(filter.getAssetName())) {
            assetQuery.addCriteria(Criteria.where("name").regex(filter.getAssetName().trim(), "i"));
        }
        if (hasText(filter.getAssetCustomer())) {
            assetQuery.addCriteria(Criteria.where("customer").regex(filter.getAssetCustomer().trim(), "i"));
        }
        if (hasText(filter.getSerialNumber())) {
            assetQuery.addCriteria(Criteria.where("serialNumber").regex(filter.getSerialNumber().trim(), "i"));
        }
        if (hasText(filter.getAssetCategory())) {
            assetQuery.addCriteria(Criteria.where("category").is(filter.getAssetCategory().trim()));
        }
        if (hasText(filter.getAssetLocation())) {
            assetQuery.addCriteria(Criteria.where("location").is(filter.getAssetLocation().trim()));
        }
        if (hasText(filter.getCustomerId())) {
            assetQuery.addCriteria(Criteria.where("customerId").is(filter.getCustomerId().trim()));
        }
        if (hasText(filter.getCustomerCategory())) {
            Query customerQuery = new Query(Criteria.where("companyId").is(companyId)
                    .and("category").is(filter.getCustomerCategory().trim()));
            customerQuery.fields().include("_id");
            List<CompanyCustomer> matchingCustomers = mongoTemplate.find(customerQuery, CompanyCustomer.class);
            List<String> customerIds = matchingCustomers.stream()
                    .map(CompanyCustomer::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (customerIds.isEmpty()) {
                return Collections.emptyList();
            }
            assetQuery.addCriteria(Criteria.where("customerId").in(customerIds));
        }

        assetQuery.fields().include("_id");
        return mongoTemplate.find(assetQuery, Assets.class).stream()
                .map(Assets::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Enrich inspection details with asset information (customerId, assetName, assetCategory, customerName, customerCategory, assetLocation)
     */
    private List<InspectionDetailResponseDTO> enrichInspectionDetails(List<AssetCategoryInspectionInstance> inspections) {
        List<InspectionDetailResponseDTO> detailedResponses = new ArrayList<>();

        // Get all unique assetIds
        Set<String> assetIds = inspections.stream()
                .map(AssetCategoryInspectionInstance::getAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Fetch all assets
        Query assetQuery = new Query(Criteria.where("id").in(assetIds));
        List<Assets> assets = mongoTemplate.find(assetQuery, Assets.class);

        Map<String, Assets> assetMap = assets.stream()
                .collect(Collectors.toMap(Assets::getId, asset -> asset, (a1, a2) -> a1));

        // Get all unique customerIds to fetch customer details
        Set<String> customerIds = assets.stream()
                .map(Assets::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Query customerQuery = new Query(Criteria.where("id").in(customerIds));
        List<CompanyCustomer> customers = mongoTemplate.find(customerQuery, CompanyCustomer.class);

        Map<String, CompanyCustomer> customerMap = customers.stream()
                .collect(Collectors.toMap(CompanyCustomer::getId, customer -> customer, (c1, c2) -> c1));

        // Build detailed responses
        for (AssetCategoryInspectionInstance inspection : inspections) {
            InspectionDetailResponseDTO detail = new InspectionDetailResponseDTO();
            detail.setInspectionInstance(inspection);

            // Get asset details
            Assets asset = assetMap.get(inspection.getAssetId());
            if (asset != null) {
                detail.setAssetName(asset.getName());
                detail.setAssetCategory(asset.getCategory());
                detail.setAssetLocation(asset.getLocation());
                detail.setSerialNumber(asset.getSerialNumber());
                detail.setAssetBusinessId(asset.getAssetId());
                detail.setCustomerId(asset.getCustomerId());
                detail.setCustomerName(asset.getCustomer());

                // Get customer category
                CompanyCustomer customer = customerMap.get(asset.getCustomerId());
                if (customer != null) {
                    detail.setCustomerCategory(customer.getCategory());
                }
            }

            detailedResponses.add(detail);
        }

        return detailedResponses;
    }
}
