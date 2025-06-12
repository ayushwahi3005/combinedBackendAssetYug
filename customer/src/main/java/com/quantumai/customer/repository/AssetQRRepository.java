package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetQR;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetQRRepository extends MongoRepository<AssetQR, String> {
  public Optional<AssetQR> findByCompanyId(Long companyId);
}
