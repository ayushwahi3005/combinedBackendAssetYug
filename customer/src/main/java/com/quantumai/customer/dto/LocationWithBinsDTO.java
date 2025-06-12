package com.quantumai.customer.dto;

import java.util.List;
import lombok.Data;

@Data
public class LocationWithBinsDTO {
  private String id;
  private String name;
  private List<SimpleBinDTO> bins;
}
