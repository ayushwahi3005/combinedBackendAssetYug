package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerMandatoryFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerMandatoryFieldsRepository
    extends MongoRepository<CompanyCustomerMandatoryFields, String> , CompanyScopedRepository{
  public Optional<CompanyCustomerMandatoryFields> findByNameAndCompanyId(
      String name, Long companyId);

  public List<CompanyCustomerMandatoryFields> findByCompanyId(Long companyId);
  public List<CompanyCustomerMandatoryFields> findByCompanyIdAndMandatory(Long companyId,Boolean mandatory);

  public void deleteByCompanyId(Long companyId);
}
