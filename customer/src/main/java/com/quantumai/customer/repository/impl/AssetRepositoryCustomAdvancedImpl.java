package com.quantumai.customer.repository.impl;

import com.quantumai.customer.dto.AssetAdvancedFilterDTO;
import com.quantumai.customer.dto.AssetCheckInOutData;
import com.quantumai.customer.dto.AssetWithCustomFieldsDTO;
import com.quantumai.customer.dto.PaginatedAssetResponseDTO;
import com.quantumai.customer.entity.AssetCheckInOut;
import com.quantumai.customer.entity.AssetExtraFields;
import com.quantumai.customer.entity.Assets;
import com.quantumai.customer.entity.CheckInOutStatus;
import com.quantumai.customer.repository.AssetCheckInOutAdvance;
import com.quantumai.customer.repository.AssetCheckInOutRepository;
import com.quantumai.customer.repository.AssetRepositoryCustomAdvanced;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.quantumai.customer.repository.AssetExtraFieldsRepository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class AssetRepositoryCustomAdvancedImpl implements AssetRepositoryCustomAdvanced {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AssetExtraFieldsRepository assetExtraFieldsRepository;

    @Autowired
    private AssetCheckInOutAdvanceImpl assetCheckInOutAdvance;

    @Autowired
    private AssetCheckInOutRepository assetCheckInOutRepository;


    @Override
    public PaginatedAssetResponseDTO getAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter) {
        log.info("Fetching assets with advanced filter - companyId: {}, sortField: {}, sortDir: {}",
                filter.getCompanyId(), filter.getSortField(), filter.getSortDirection());

        try {
            // Build query for predefined fields
            Query query = buildQuery(filter);

            if (!filter.getCustomFields().isEmpty()) {
                filter.getCustomFields().entrySet().removeIf(
                        entry -> entry.getValue() == null || entry.getValue().trim().isEmpty()
                );
            }
            log.info("Custom fields after{}",filter.getCustomFields().toString());
            // Add custom field filter results (intersection)
            if (filter.hasCustomFieldFilters()) {
                Set<String> customFieldAssetIds = searchAssetbyCustomField(filter);
                log.info("customFieldAssetIds : {}",customFieldAssetIds.toString());
//                Set<String> customFieldAssetIds = customFieldResults.stream()
//                        .map(Assets::getId)
//                        .collect(Collectors.toSet());

                if (customFieldAssetIds.isEmpty()) {
                    // No assets match custom field criteria
                    return new PaginatedAssetResponseDTO(
                            new ArrayList<>(),
                            0,
                            0,
                            filter.getPageNumber(),
                            filter.getPageSize(),
                            false,
                            false
                    );
                }

                // Add intersection criteria
                query.addCriteria(Criteria.where("id").in(customFieldAssetIds));
            }

            String effectiveSortField = filter.getEffectiveSortField();
            Sort.Direction direction = "DESC".equalsIgnoreCase(filter.getSortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            Sort sort = Sort.by(direction, effectiveSortField);
            query.with(sort);

            // Get total count before pagination
            long totalCount = mongoTemplate.count(query, Assets.class);

            // Apply pagination
            query.skip((long) filter.getPageNumber() * filter.getPageSize())
                    .limit(filter.getPageSize());

            List<Assets> assets = mongoTemplate.find(query, Assets.class);



            List<CheckInOutStatus> listOfCheckInOutStatus = assetCheckInOutAdvance.getCheckInOutStatusByAssetIds(
                    assets.stream().map(Assets::getId).collect(Collectors.toList())
            );
            Query checkInOutQuery=new Query(Criteria.where("assetId").in(assets.stream().map(Assets::getId).collect(Collectors.toList())));
            List<AssetCheckInOut> assetCheckInOutDataList=mongoTemplate.find(checkInOutQuery,AssetCheckInOut.class);

            // Batch fetch all custom fields at once
            List<String> assetIds = assets.stream().map(Assets::getId).collect(Collectors.toList());
            Map<String, List<AssetExtraFields>> customFieldsMap = fetchAllCustomFields(assetIds);

            // Convert to DTO and add custom fields
            List<AssetWithCustomFieldsDTO> assetDTOs = assets.stream()
                    .map(asset -> convertToDTO(asset, filter, customFieldsMap))
                    .toList();

            List<AssetWithCustomFieldsDTO> assetDTOAfterAddingCheckInOut = assetDTOs.stream()
                    .map(asset -> {
                        Optional<CheckInOutStatus> statusOpt = listOfCheckInOutStatus.stream()
                                .filter(status -> status.getAssetId().equals(asset.getId()))
                                .findFirst();
                        Optional<AssetCheckInOut> assetCheckInOutOptional = assetCheckInOutDataList.stream()
                                .filter(status -> status.getAssetId().equals(asset.getId()))
                                .findFirst();
                        assetCheckInOutOptional.ifPresentOrElse(asset::setAssetCheckInOut,
                                ()->asset.setAssetCheckInOut(null));

                        statusOpt.ifPresentOrElse(
                                status -> asset.setCheckedInOutStatus(status.getStatus()),
                                () -> asset.setCheckedInOutStatus("Checked In")


                        );
                        return asset;
                    })
                    .toList();

            int totalPages = (int) Math.ceil((double) totalCount / filter.getPageSize());

            return new PaginatedAssetResponseDTO(
                    assetDTOAfterAddingCheckInOut,
                    totalCount,
                    totalPages,
                    filter.getPageNumber(),
                    filter.getPageSize(),
                    filter.getPageNumber() < totalPages - 1,
                    filter.getPageNumber() > 0
            );

        } catch (Exception e) {
            log.error("Error fetching assets with advanced filter", e);
            return new PaginatedAssetResponseDTO(
                    new ArrayList<>(),
                    0,
                    0,
                    filter.getPageNumber(),
                    filter.getPageSize(),
                    false,
                    false
            );
        }
    }

    @Override
    public long countAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter) {
        try {
            Query query = buildQuery(filter);
            return mongoTemplate.count(query, Assets.class);
        } catch (Exception e) {
            log.error("Error counting assets with advanced filter", e);
            return 0;
        }
    }

    @Override
    public Page<Assets> findByCompanyIdWithSort(Long companyId, String sortField,
                                                String sortDirection, Pageable pageable) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("companyId").is(companyId));

            Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            Sort sort = Sort.by(direction, sortField);
            query.with(sort);

            query.skip((long) pageable.getPageNumber() * pageable.getPageSize())
                    .limit(pageable.getPageSize());

            List<Assets> assets = mongoTemplate.find(query, Assets.class);
            long count = mongoTemplate.count(
                    Query.query(Criteria.where("companyId").is(companyId)),
                    Assets.class
            );

            return new PageImpl<>(assets, pageable, count);
        } catch (Exception e) {
            log.error("Error fetching assets with sort", e);
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }

    private Query buildQuery(AssetAdvancedFilterDTO filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Always filter by companyId
        criteriaList.add(Criteria.where("companyId").is(filter.getCompanyId()));

        // Add optional predefined field filters
        if (filter.getAssetId() != null && !filter.getAssetId().isEmpty()) {
            try {
                criteriaList.add(Criteria.where("assetId").is(Integer.parseInt(filter.getAssetId())));
            } catch (NumberFormatException e) {
                log.warn("Invalid assetId: {}", filter.getAssetId());
            }
        }

        if (filter.getName() != null && !filter.getName().isEmpty()) {
            criteriaList.add(Criteria.where("name")
                    .regex(".*" + escapeRegex(filter.getName()) + ".*", "i"));
        }

        if (filter.getCustomer() != null && !filter.getCustomer().isEmpty()) {
            criteriaList.add(Criteria.where("customer")
                    .regex(".*" + escapeRegex(filter.getCustomer()) + ".*", "i"));
        }

        if (filter.getSerialNumber() != null && !filter.getSerialNumber().isEmpty()) {
            criteriaList.add(Criteria.where("serialNumber")
                    .regex(".*" + escapeRegex(filter.getSerialNumber()) + ".*", "i"));
        }

        if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
            criteriaList.add(Criteria.where("category")
                    .regex(".*" + escapeRegex(filter.getCategory()) + ".*", "i"));
        }

        if (filter.getLocation() != null && !filter.getLocation().isEmpty()) {
            criteriaList.add(Criteria.where("location").is(filter.getLocation()));
        }

        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            criteriaList.add(Criteria.where("status")
                    .regex(".*" + escapeRegex(filter.getStatus()) + ".*", "i"));
        }

        if (filter.getEmail() != null && !filter.getEmail().isEmpty()) {
            criteriaList.add(Criteria.where("email")
                    .regex(".*" + escapeRegex(filter.getEmail()) + ".*", "i"));
        }

        Query query = new Query();
        for (Criteria criteria : criteriaList) {
            query.addCriteria(criteria);
        }
        return query;
    }
    private Map<String, List<AssetExtraFields>> fetchAllCustomFields(List<String> assetIds) {
        // Fetch all custom fields for all assets in one query
        return assetExtraFieldsRepository.findByAssetIdIn(assetIds)
                .stream()
                .collect(Collectors.groupingBy(AssetExtraFields::getAssetId));
    }

    private AssetWithCustomFieldsDTO convertToDTO(Assets asset, AssetAdvancedFilterDTO filter,
                                                  Map<String, List<AssetExtraFields>> allCustomFields) {
        AssetWithCustomFieldsDTO dto = new AssetWithCustomFieldsDTO();

        dto.setId(asset.getId());
        dto.setAssetId(asset.getAssetId());
        dto.setName(asset.getName());
        dto.setSerialNumber(asset.getSerialNumber());
        dto.setCategory(asset.getCategory());
        dto.setCustomer(asset.getCustomer());
        dto.setCustomerId(asset.getCustomerId());
        dto.setLocation(asset.getLocation());
        dto.setStatus(asset.getStatus());
        dto.setEmail(asset.getEmail());
        dto.setImage(asset.getImage());
        dto.setCompanyId(asset.getCompanyId());
        dto.setUpdatedAt(asset.getUpdatedAt());

        // Fetch and add custom fields
        Map<String, String> customFieldsMap = new HashMap<>();


        List<AssetExtraFields> extraFields = allCustomFields.getOrDefault(asset.getId(), new ArrayList<>());

        for (var field : extraFields) {
            customFieldsMap.put(field.getName(), field.getValue() != null ? field.getValue() : "");
        }

        dto.setCustomFields(customFieldsMap);
        return dto;
    }

//    private AssetWithCustomFieldsDTO convertToDTO(Assets asset, AssetAdvancedFilterDTO filter) {
//        AssetWithCustomFieldsDTO dto = new AssetWithCustomFieldsDTO();
//
//        dto.setId(asset.getId());
//        dto.setAssetId(asset.getAssetId());
//        dto.setName(asset.getName());
//        dto.setSerialNumber(asset.getSerialNumber());
//        dto.setCategory(asset.getCategory());
//        dto.setCustomer(asset.getCustomer());
//        dto.setCustomerId(asset.getCustomerId());
//        dto.setLocation(asset.getLocation());
//        dto.setStatus(asset.getStatus());
//        dto.setEmail(asset.getEmail());
//        dto.setImage(asset.getImage());
//        dto.setCompanyId(asset.getCompanyId());
//        dto.setUpdatedAt(asset.getUpdatedAt());
//
//        // Fetch and add custom fields
//        Map<String, String> customFieldsMap = new HashMap<>();
//        try {
//            var extraFields = assetExtraFieldsRepository.findByAssetId(asset.getId());
//            if (extraFields != null && !extraFields.isEmpty()) {
//                for (var field : extraFields) {
//                    customFieldsMap.put(field.getName(), field.getValue() != null ? field.getValue() : "");
//                }
//            }
//        } catch (Exception e) {
//            log.debug("Error fetching custom fields for asset {}", asset.getId());
//        }
//
//        dto.setCustomFields(customFieldsMap);
//        return dto;
//    }

    private String escapeRegex(String str) {
        if(str!=null) {
            return str.replaceAll("([.?*+^$\\[\\]\\\\(){}|])", "\\\\$1");
        }
        return "";
    }

    private Set<String> searchAssetbyCustomField(AssetAdvancedFilterDTO filter) {
        try {

            Query baseQuery = new Query();
            baseQuery.addCriteria(
                    Criteria.where("companyId").is(filter.getCompanyId())
            );

            Map<String, String> customFields = filter.getCustomFields();



            // If no custom fields provided → return all
            if (customFields == null || customFields.isEmpty()) {
                return mongoTemplate.find(baseQuery, AssetExtraFields.class)
                        .stream()
                        .map(AssetExtraFields::getAssetId)
                        .collect(Collectors.toSet());
            }

            Query customFieldQuery = new Query();
            customFieldQuery.addCriteria(
                    Criteria.where("companyId").is(filter.getCompanyId())
            );

            boolean hasValidFilter = false;

            for (Map.Entry<String, String> entry : customFields.entrySet()) {

                String key = entry.getKey();
                String value = entry.getValue();

                // Treat null / empty / only spaces as no filter
                if (value != null && !value.trim().isEmpty()) {
                    hasValidFilter = true;

                    customFieldQuery.addCriteria(
                            Criteria.where("name").is(key)
                                    .and("value")
                                    .regex(".*" + escapeRegex(value.trim()) + ".*", "i")
                    );
                }
            }

            // If no valid filter values → return all AssetExtraFields
            if (!hasValidFilter) {
                return mongoTemplate.find(baseQuery, AssetExtraFields.class)
                        .stream()
                        .map(AssetExtraFields::getAssetId)
                        .collect(Collectors.toSet());
            }

            return mongoTemplate.find(customFieldQuery, AssetExtraFields.class)
                    .stream()
                    .map(AssetExtraFields::getAssetId)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("Error searching assets by custom field", e);
            return Collections.emptySet();
        }
    }


}

