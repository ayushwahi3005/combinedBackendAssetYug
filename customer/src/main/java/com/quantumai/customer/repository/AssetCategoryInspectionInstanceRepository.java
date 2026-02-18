package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCategoryInspectionInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetCategoryInspectionInstanceRepository
    extends MongoRepository<AssetCategoryInspectionInstance, String> , CompanyScopedRepository{

  public List<AssetCategoryInspectionInstance> findByAssetId(String assetId);

  public Optional<AssetCategoryInspectionInstance> findByAssetCategoryInspectionId(
      String assetCategoryInspectionId);

  public List<AssetCategoryInspectionInstance> findByCompanyId(Long companyId);
  List<AssetCategoryInspectionInstance> findByCompanyIdAndUpdatedAtBetween(
          Long companyId,
          LocalDateTime startDate,
          LocalDateTime endDate
  );

  public void deleteByCompanyId(Long companyId);
}
