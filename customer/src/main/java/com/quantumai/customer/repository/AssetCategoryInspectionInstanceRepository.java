package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetCategoryInspectionInstanceRepository extends MongoRepository<AssetCategoryInspectionInstance, String> {

    public Optional<AssetCategoryInspectionInstance> findByAssetId(String assetId);
    public Optional<AssetCategoryInspectionInstance> findByAssetCategoryInspectionId(String assetCategoryInspectionId);
    public List<AssetCategoryInspectionInstance> findByCompanyId(String companyId);
}
