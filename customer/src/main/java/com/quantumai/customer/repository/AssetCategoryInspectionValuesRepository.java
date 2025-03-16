package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetCategoryInspectionValues;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetCategoryInspectionValuesRepository extends MongoRepository<AssetCategoryInspectionValues, String> {

    public Optional<AssetCategoryInspectionValues> findByAssetId(String assetId);
    public Optional<AssetCategoryInspectionValues> findByAssetCategoryInspectionId(String assetCategoryInspectionId);
    public List<AssetCategoryInspectionValues> findByCompanyId(String companyId);
}
