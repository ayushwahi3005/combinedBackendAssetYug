package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCheckInOut;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetCheckInOutRepository extends MongoRepository<AssetCheckInOut, String> {
  public Optional<AssetCheckInOut> findByAssetId(String assetid);

  public List<AssetCheckInOut> findByCompanyId(String companyId);
}
