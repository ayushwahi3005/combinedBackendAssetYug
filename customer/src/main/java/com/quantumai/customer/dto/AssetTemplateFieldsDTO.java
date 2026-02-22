package com.quantumai.customer.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssetTemplateFieldsDTO {
  private List<String> standardFields;
  private List<String> extraFields;
}
