package com.quantumai.customer.repository.impl;

import com.quantumai.customer.dto.AssetCountByCategoryDTO;
import com.quantumai.customer.repository.AssetCountByCategoriesRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
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
public class AssetCountByCategoriesRepositoryImpl implements AssetCountByCategoriesRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<AssetCountByCategoryDTO> getAssetCountByCategories(Long companyId, String sortOrder) {
        try {
            log.info("Fetching asset count by categories for companyId: {}, sortOrder: {}", companyId, sortOrder);

            List<AggregationOperation> operations = new ArrayList<>();

            // Stage 1: Match - Filter assets by companyId
            operations.add(match(Criteria.where("companyId").is(companyId)));

            // Stage 2: Group - Group by category name and count assets
            operations.add(group("$category")
                    .count().as("assetCount"));

            // Stage 3: Lookup - Join with AssetCategory collection to get category details
            operations.add(lookup("assetCategory", "_id", "name", "categoryDetails"));

            // Stage 4: Unwind - Unwind the categoryDetails array
            operations.add(unwind("$categoryDetails", true));

            // Stage 5: Match - Filter out records where categoryDetails is empty/null
            operations.add(match(Criteria.where("categoryDetails").exists(true).ne(null)));

            // Stage 6: AddFields - Convert ObjectId to String to avoid ConverterNotFoundException
            operations.add(context -> new Document("$addFields",
                    new Document("categoryIdStr",
                            new Document("$toString", "$categoryDetails._id"))));

            // Stage 7: Project - Select and rename fields
            operations.add(project()
                    .and("categoryIdStr").as("assetCategoryId")
                    .and("categoryDetails.name").as("categoryName")
                    .and("categoryDetails.status").as("categoryStatus")
                    .and("assetCount").as("assetCount"));

            // Stage 8: Sort - Sort by assetCount
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
            AggregationResults<AssetCountByCategoryDTO> results = mongoTemplate.aggregate(
                    aggregation,
                    "assets",
                    AssetCountByCategoryDTO.class
            );

            List<AssetCountByCategoryDTO> resultList = results.getMappedResults();
            log.info("Successfully fetched asset counts for {} categories", resultList.size());
            resultList.forEach(dto -> log.info("Category: {}, Asset Count: {}", dto.getCategoryName(), dto.getAssetCount()));
            return resultList;

        } catch (Exception e) {
            log.error("Error fetching asset count by categories: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching asset count by categories", e);
        }
    }
}