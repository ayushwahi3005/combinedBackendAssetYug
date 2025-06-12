package com.quantumai.customer.entity;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AssetCategoryInspection {

  @Id private String id;
  private String name;
  private String categoryName;
  private String categoryId;
  private Long companyId;
  private List<InspectionStep> steps;
  private StatusEnum status;
}
