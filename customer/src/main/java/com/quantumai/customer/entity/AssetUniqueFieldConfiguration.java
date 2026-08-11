package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

@Document
@Data
@CompoundIndexes({
  @CompoundIndex(name = "company_field_idx", def = "{'companyId': 1, 'fieldName': 1}", unique = true)
})
public class AssetUniqueFieldConfiguration {

  @Id private String id;
  private Long companyId;
  private String fieldName;
  private Boolean isUnique;
  private String email;
  private String type; // STANDARD or EXTRA
  private String createdAt;
  private String updatedAt;

  private String createdBy;
  private String lastUpdatedBy;
}
