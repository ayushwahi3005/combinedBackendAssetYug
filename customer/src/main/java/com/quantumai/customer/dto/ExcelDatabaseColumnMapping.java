package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class ExcelDatabaseColumnMapping {

  private String excelColumn;
  private String databaseColumn;
}
