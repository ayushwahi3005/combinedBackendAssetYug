package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class AssetFileDTO {
  private String id;
  private String assetId;
  private byte[] file;
  private String fileName;
}
