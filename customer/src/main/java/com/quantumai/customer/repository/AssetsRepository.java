package com.quantumai.customer.repository;

import com.quantumai.customer.dto.AssetsDTO;
import com.quantumai.customer.entity.Assets;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AssetsRepository extends MongoRepository<Assets, String>, CompanyScopedRepository, AssetRepositoryCustomAdvanced{
  List<Assets> findByCompanyId(Long companyId);
  List<Assets> findByCompanyIdAndStatus(Long companyId,String status);
  List<Assets> findByCustomerId(String customerId);

  Optional<Assets> findByAssetIdAndCompanyId(Integer assetId, Long companyId);

  List<AssetsDTO> findByCompanyIdAndSerialNumber(Long companyId, String serialNumber);

  public int countByCategoryIgnoreCase(String category);

//  List<Assets> findByCompanyIdAndLocationId(Long companyId, String locationId);
  List<Assets> findByCompanyIdAndLocation(Long companyId, String location);

  public void deleteByCompanyId(Long companyId);



}
