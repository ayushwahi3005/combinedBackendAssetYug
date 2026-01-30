package com.quantumai.customer.repository;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyCustomerRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public long countActiveAssetsWithExtraField(String fieldName, Long companyId) {

        MatchOperation matchCompany =
                Aggregation.match(Criteria.where("companyId").is(companyId));

//        MatchOperation matchActiveAssets =
//                Aggregation.match(Criteria.where("status").is("active"));

        // Add a field to convert _id to string
        AddFieldsOperation addStringId = Aggregation.addFields()
                .addField("idAsString")
                .withValueOfExpression("{ $toString: '$_id' }")
                .build();

        LookupOperation lookupExtraFields =
                LookupOperation.newLookup()
                        .from("companyCustomerExtraFields")
                        .localField("idAsString")    // Use string version
                        .foreignField("companyCustomerId")
                        .as("extraFields");

        MatchOperation matchExtraFieldConditions =
                Aggregation.match(
                        Criteria.where("extraFields").elemMatch(
                                Criteria.where("name").is(fieldName)
                                        .and("companyId").is(companyId)
                                        .and("value").ne(null).ne("")
                        )
                );

        CountOperation countOperation =
                Aggregation.count().as("total");

        Aggregation aggregation = Aggregation.newAggregation(
                matchCompany,
                addStringId,           // ✅ Add this
                lookupExtraFields,
                matchExtraFieldConditions,
                countOperation
        );

        AggregationResults<Document> result =
                mongoTemplate.aggregate(aggregation, "companyCustomer", Document.class);

        Document document = result.getUniqueMappedResult();
        return document != null ? document.getInteger("total") : 0;
    }


}