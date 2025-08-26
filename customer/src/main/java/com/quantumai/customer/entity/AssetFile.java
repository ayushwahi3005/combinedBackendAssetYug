package com.quantumai.customer.entity;

import lombok.Data;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AssetFile {

  @Id private String id;
  private String assetId;
  private String fileName;
  private LocalDateTime uploadDateTime;
  private byte[] file;
}
