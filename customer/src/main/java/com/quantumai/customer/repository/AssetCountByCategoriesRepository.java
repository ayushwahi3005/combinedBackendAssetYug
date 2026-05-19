package com.quantumai.customer.repository;

import com.quantumai.customer.dto.AssetCountByCategoryDTO;

import java.util.List;

public interface AssetCountByCategoriesRepository {

    List<AssetCountByCategoryDTO> getAssetCountByCategories(Long companyId, String sortOrder);


}
