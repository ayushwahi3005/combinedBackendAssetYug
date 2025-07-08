package com.quantumai.customer.repository;

import com.quantumai.customer.dto.AssetsDTO;
import com.quantumai.customer.entity.Assets;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetsRepository extends MongoRepository<Assets, String> {
  List<Assets> findByCompanyId(Long companyId);

  List<Assets> findByCustomerId(String customerId);

  Assets findByAssetIdAndCompanyId(Integer assetId, Long companyId);

  List<AssetsDTO> findByCompanyIdAndSerialNumber(Long companyId, String serialNumber);

  public int countByCategoryIgnoreCase(String category);
}
