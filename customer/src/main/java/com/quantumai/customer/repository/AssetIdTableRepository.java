package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.AssetIdTable;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetIdTableRepository extends MongoRepository<AssetIdTable, String> {
  public Optional<AssetIdTable> findByCompanyId(Long id);
}
