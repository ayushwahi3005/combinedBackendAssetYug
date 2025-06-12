package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetExtraFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetExtraFieldsRepository extends MongoRepository<AssetExtraFields, String> {
  public List<AssetExtraFields> findByAssetId(String assetId);

  public List<AssetExtraFields> findByCompanyId(Long companyId);

  public List<AssetExtraFields> findByName(String name);

  public Optional<AssetExtraFields> findByNameAndAssetId(String name, String assetId);
}
