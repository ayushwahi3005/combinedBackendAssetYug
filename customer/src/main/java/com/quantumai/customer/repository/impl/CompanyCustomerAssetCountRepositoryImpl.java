package com.quantumai.customer.repository.impl;

import com.quantumai.customer.dto.AssetCountByCompanyCustomerDTO;
import com.quantumai.customer.repository.CompanyCustomerAssetCountRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
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

            List<AggregationOperation> operations = new ArrayList<>();

            // Stage 1: Match by companyId only (include ALL assets, even without customerId)
            operations.add(match(Criteria.where("companyId").is(companyId)));

            // Stage 2: Group by customerId (null customerId will be grouped under _id: null)
            operations.add(group("$customerId").count().as("assetCount"));

            // Stage 3: Safely convert String customerId to ObjectId only if valid 24-char hex
            operations.add(context -> new Document("$addFields",
                    new Document("customerObjectId",
                            new Document("$cond", new Document()
                                    .append("if", new Document("$regexMatch", new Document()
                                            .append("input", new Document("$ifNull", Arrays.asList("$_id", "")))
                                            .append("regex", "^[0-9a-fA-F]{24}$")
                                    ))
                                    .append("then", new Document("$toObjectId", "$_id"))
                                    .append("else", null)
                            ))));

            // Stage 4: Lookup companyCustomer by ObjectId (returns empty array if null)
            operations.add(context -> new Document("$lookup",
                    new Document("from", "companyCustomer")
                            .append("localField", "customerObjectId")
                            .append("foreignField", "_id")
                            .append("as", "customerDetails")));

            // Stage 5: Unwind with preserveNullAndEmptyArrays = true (keep unassigned assets)
            operations.add(unwind("$customerDetails", true));

            // Stage 6: Project final fields with null-safe fallbacks
            operations.add(context -> new Document("$project",
                    new Document("companyCustomerId",
                            new Document("$ifNull", Arrays.asList(
                                    new Document("$toString", new Document("$ifNull", Arrays.asList("$customerDetails.companyCustomerId", ""))),
                                    null
                            )))
                            .append("companyCustomerName",
                                    new Document("$ifNull", Arrays.asList("$customerDetails.name", "Unassigned")))
                            .append("email",
                                    new Document("$ifNull", Arrays.asList("$customerDetails.email", null)))
                            .append("assetCount", 1L)));

            // Stage 7: Sort by assetCount
            String sortOrderUpper = sortOrder != null ? sortOrder.toUpperCase() : "DESC";
            if ("ASC".equals(sortOrderUpper)) {
                operations.add(sort(Sort.by(Sort.Order.asc("assetCount"))));
            } else {
                operations.add(sort(Sort.by(Sort.Order.desc("assetCount"))));
            }

            Aggregation aggregation = newAggregation(operations);
            AggregationResults<AssetCountByCompanyCustomerDTO> results = mongoTemplate.aggregate(
                    aggregation,
                    "assets",
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
