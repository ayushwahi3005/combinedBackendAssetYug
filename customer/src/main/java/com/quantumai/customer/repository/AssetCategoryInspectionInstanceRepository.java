package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetCategoryInspectionInstanceRepository
    extends MongoRepository<AssetCategoryInspectionInstance, String> {

  public List<AssetCategoryInspectionInstance> findByAssetId(String assetId);

  public Optional<AssetCategoryInspectionInstance> findByAssetCategoryInspectionId(
      String assetCategoryInspectionId);

  public List<AssetCategoryInspectionInstance> findByCompanyId(Long companyId);
}
