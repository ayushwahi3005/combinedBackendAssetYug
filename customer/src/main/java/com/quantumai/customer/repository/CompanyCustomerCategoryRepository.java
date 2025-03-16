package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerCategoryRepository
    extends MongoRepository<CompanyCustomerCategory, String> {
  public List<CompanyCustomerCategory> findByCompanyId(String companyId);

  public List<CompanyCustomerCategory> findByCompanyIdAndStatus(String companyId, String status);

  public Optional<CompanyCustomerCategory> findByName(String name);

}
