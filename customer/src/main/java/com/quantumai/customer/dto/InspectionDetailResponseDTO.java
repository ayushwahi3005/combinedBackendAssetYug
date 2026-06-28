package com.quantumai.customer.dto;

import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDetailResponseDTO {
    private AssetCategoryInspectionInstance inspectionInstance;
    private String customerId;
    private String assetName;
    private String assetCategory;
    private String customerName;
    private String customerCategory;
    private String assetLocation;
    private String serialNumber;
    private Integer assetBusinessId;
}
