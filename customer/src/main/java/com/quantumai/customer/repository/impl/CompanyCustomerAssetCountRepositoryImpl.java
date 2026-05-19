package com.quantumai.customer.repository.impl;

import com.quantumai.customer.dto.AssetCountByCompanyCustomerDTO;
import com.quantumai.customer.repository.CompanyCustomerAssetCountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@Slf4j
public class CompanyCustomerAssetCountRepositoryImpl implements CompanyCustomerAssetCountRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<AssetCountByCompanyCustomerDTO> getAssetCountByCompanyCustomer(Long companyId, String sortOrder) {
        try {
            log.info("Fetching asset count by company customer for companyId: {}, sortOrder: {}", companyId, sortOrder);

            // Build aggregation pipeline
            List<AggregationOperation> operations = new ArrayList<>();

            // Stage 1: Match - Filter assets by companyId
            operations.add(match(Criteria.where("companyId").is(companyId)));

            // Stage 2: Group - Group by customerId and count assets
            operations.add(group("$customerId")
                    .count().as("assetCount"));

            // Stage 3: Lookup - Join with CompanyCustomer collection to get customer details
            // Join on _id (which is the customerId from Stage 2 grouping) with CompanyCustomer.id
            operations.add(lookup("companyCustomer", "_id", "id", "customerDetails"));

            // Stage 4: Unwind - Unwind the customerDetails array
            operations.add(unwind("$customerDetails", true));

            // Stage 4.5: Match - Filter out records where customerDetails is empty/null
            operations.add(match(Criteria.where("customerDetails").exists(true).ne(null)));

            // Stage 5: Project - Select and rename fields
            operations.add(project()
                    .and("customerDetails.companyCustomerId").as("companyCustomerId")
                    .and("customerDetails.name").as("companyCustomerName")
                    .and("customerDetails.email").as("email")
                    .and("assetCount").as("assetCount"));

            // Stage 6: Sort - Sort by assetCount
            String sortOrderUpper = sortOrder != null ? sortOrder.toUpperCase() : "DESC";
            if ("ASC".equals(sortOrderUpper)) {
                operations.add(sort(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.asc("assetCount"))));
            } else {
                operations.add(sort(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("assetCount"))));
            }

            // Create and execute aggregation
            Aggregation aggregation = newAggregation(operations);
            AggregationResults<AssetCountByCompanyCustomerDTO> results = mongoTemplate.aggregate(
                    aggregation,
                    "assets", // Source collection name (lowercase)
                    AssetCountByCompanyCustomerDTO.class
            );

            List<AssetCountByCompanyCustomerDTO> resultList = results.getMappedResults();
            log.info("Successfully fetched {} asset count records", resultList.size());
            return resultList;

        } catch (Exception e) {
            log.error("Error fetching asset count by company customer: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching asset count by company customer", e);
        }
    }
}
