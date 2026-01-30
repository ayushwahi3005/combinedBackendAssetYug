package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerExtraFields;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CompanyCustomerExtraFieldsRepository
    extends MongoRepository<CompanyCustomerExtraFields, String> , CompanyScopedRepository{
  public List<CompanyCustomerExtraFields> findByCompanyCustomerId(String workorderId);

  public List<CompanyCustomerExtraFields> findByCompanyId(Long companyId);

  public List<CompanyCustomerExtraFields> findByName(String name);

  public CompanyCustomerExtraFields findByNameIgnoreCaseAndCompanyCustomerId(
      String name, String companyCustomerId);

  public List<CompanyCustomerExtraFields> findByNameIgnoreCaseAndCompanyId(
      String companyCustomerName, Long companyId);

  public void deleteByCompanyId(Long companyId);



}
