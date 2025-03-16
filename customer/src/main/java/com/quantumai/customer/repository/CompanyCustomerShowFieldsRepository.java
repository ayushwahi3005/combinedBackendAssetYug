package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerShowFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerShowFieldsRepository
    extends MongoRepository<CompanyCustomerShowFields, String> {
  public Optional<CompanyCustomerShowFields> findByNameAndCompanyId(String name, String companyId);

  public List<CompanyCustomerShowFields> findByCompanyId(String companyId);
}
