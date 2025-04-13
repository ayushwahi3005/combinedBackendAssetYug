package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class AssetCategoryInspectionInstance {
    @Id
    private String id;
    private String assetId;
    private String companyId;
    private String assetCategoryInspectionId;
    private List<InspectionStepValues> stepValues;
}
