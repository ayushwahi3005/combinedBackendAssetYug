package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class AssetCountByCategoryDTO {

    private String assetCategoryId;
    private String categoryName;
    private String categoryStatus;
    private Integer assetCount;
}
