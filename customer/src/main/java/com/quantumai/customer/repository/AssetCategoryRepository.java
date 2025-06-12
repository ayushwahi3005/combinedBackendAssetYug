package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetCategoryRepository extends MongoRepository<AssetCategory, String> {
  public List<AssetCategory> findByCompanyId(Long companyId);

  public Optional<AssetCategory> findByName(String name);

  public List<AssetCategory> findByCompanyIdAndStatus(Long companyId, String status);
}
