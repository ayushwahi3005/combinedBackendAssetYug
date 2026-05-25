package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetUniqueFieldConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetUniqueFieldConfigurationRepository extends MongoRepository<AssetUniqueFieldConfiguration, String> {

  List<AssetUniqueFieldConfiguration> findByCompanyId(Long companyId);

  Optional<AssetUniqueFieldConfiguration> findByCompanyIdAndFieldName(Long companyId, String fieldName);

  List<AssetUniqueFieldConfiguration> findByCompanyIdAndIsUniqueTrue(Long companyId);

  List<AssetUniqueFieldConfiguration> findByCompanyIdAndIsUniqueTrueAndType(Long companyId, String type);

  void deleteByCompanyIdAndFieldName(Long companyId, String fieldName);
}
