package com.quantumai.customer.entity;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AssetCategoryInspectionInstance {
  @Id private String id;
  private String assetId;
  private Long companyId;
  private String date;
  private String actionPerformedBy;
  private String notes;

  private String assetCategoryInspectionId;
  private String assetCategoryInspectionName;
  private List<InspectionStepValues> stepValues;
}
