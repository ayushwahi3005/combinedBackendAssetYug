package com.quantumai.customer.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AssetUniqueFieldValidationDTO {
  private boolean isValid;
  private String message;
  private Map<String, List<String>> conflicts; // fieldName -> list of conflicting values/assetIds
}
