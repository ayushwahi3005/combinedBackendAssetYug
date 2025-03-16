package com.quantumai.customer.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class AssetCategoryInspection {

    @Id
    private String id;
    private String name;
    private String categoryId;
    private String companyId;
    private List<InspectionStep> steps;

}
