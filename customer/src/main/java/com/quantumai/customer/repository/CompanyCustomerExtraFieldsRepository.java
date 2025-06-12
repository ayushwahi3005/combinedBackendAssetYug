package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerExtraFields;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerExtraFieldsRepository
    extends MongoRepository<CompanyCustomerExtraFields, String> {
  public List<CompanyCustomerExtraFields> findByCompanyCustomerId(String workorderId);

  public List<CompanyCustomerExtraFields> findByCompanyId(Long companyId);

  public List<CompanyCustomerExtraFields> findByName(String name);

  public CompanyCustomerExtraFields findByNameIgnoreCaseAndCompanyCustomerId(
      String name, String companyCustomerId);

  public CompanyCustomerExtraFields findByCompanyCustomerIdAndCompanyId(
      Integer companyCustomerId, String id);
}
