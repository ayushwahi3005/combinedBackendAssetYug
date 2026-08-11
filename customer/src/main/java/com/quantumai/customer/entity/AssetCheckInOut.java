package com.quantumai.customer.entity;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AssetCheckInOut {

  @Id private String id;
  private String assetId;
  private String status;
  private Long companyId;
  private List<AssetCheckInOutDetails> detailsList;

  private String createdBy;
  private String lastUpdatedBy;
}
