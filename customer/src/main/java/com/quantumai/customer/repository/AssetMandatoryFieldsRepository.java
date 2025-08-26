package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetMandatoryFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetMandatoryFieldsRepository
    extends MongoRepository<AssetMandatoryFields, String> {
  public Optional<AssetMandatoryFields> findByNameAndCompanyId(String name, Long companyId);

  public List<AssetMandatoryFields> findByCompanyIdAndMandatory(Long companyId, boolean mandatory);

  public List<AssetMandatoryFields> findByCompanyId(Long companyId);
}
