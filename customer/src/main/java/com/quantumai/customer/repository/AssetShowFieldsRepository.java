package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetShowFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetShowFieldsRepository extends MongoRepository<AssetShowFields, String> , CompanyScopedRepository{
  public Optional<AssetShowFields> findByNameAndCompanyId(String name, Long companyId);

  public List<AssetShowFields> findByCompanyId(Long companyId);

  public void deleteByCompanyId(Long companyId);
}
