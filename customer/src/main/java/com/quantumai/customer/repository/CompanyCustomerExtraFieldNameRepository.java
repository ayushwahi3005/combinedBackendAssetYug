package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerExtraFieldName;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerExtraFieldNameRepository
    extends MongoRepository<CompanyCustomerExtraFieldName, String> , CompanyScopedRepository{
  public CompanyCustomerExtraFieldName findByName(String name);

  public List<CompanyCustomerExtraFieldName> findByCompanyId(Long companyId);
  public Optional<CompanyCustomerExtraFieldName> findByIdAndNameIgnoreCase(String id,String name);

  public CompanyCustomerExtraFieldName findByNameIgnoreCaseAndCompanyId(String name, Long companyId);

  public void deleteByCompanyId(Long companyId);
}
