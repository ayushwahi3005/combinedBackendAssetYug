package com.quantumai.customer.dto;

import java.util.List;
import lombok.Data;

@Data
public class AssetCheckInOutDTO {

  private String id;
  private String assetId;
  private String status;
  private List<AssetCheckInOutDetailsDTO> detailsList;
  private String companyId;
}
