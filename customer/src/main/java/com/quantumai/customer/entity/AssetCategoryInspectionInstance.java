package com.quantumai.customer.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AssetCategoryInspectionInstance {
  @Id private String id;
  private Long assetCategoryInspectionInstanceId;
  private String assetId;
  private Long companyId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;
  private String actionPerformedBy;
  private String notes;
  private InspectionInstanceStatus status;
  private String assetCategoryInspectionId;
  private String assetCategoryInspectionName;
  private List<InspectionStepValues> stepValues;
  private List<InspectionTemplateResult> inspectionTemplates;

  private List<SelectedItem> selectedItemList;
}
