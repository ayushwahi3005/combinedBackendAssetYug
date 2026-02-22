package com.quantumai.customer.dto;

import lombok.Data;
import java.util.List;

@Data
public class CompanyCustomerTemplateFieldsDTO {
  private List<String> standardFields;
  private List<String> extraFields;
  private List<String> categories;
}
