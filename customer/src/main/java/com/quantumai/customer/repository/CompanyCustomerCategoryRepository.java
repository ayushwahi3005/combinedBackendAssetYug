package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerCategoryRepository
    extends MongoRepository<CompanyCustomerCategory, String> , CompanyScopedRepository{
  public List<CompanyCustomerCategory> findByCompanyId(Long companyId);

  public List<CompanyCustomerCategory> findByCompanyIdAndStatus(Long companyId, String status);

  public Optional<CompanyCustomerCategory> findByName(String name);

  public Optional<CompanyCustomerCategory> findByNameAndCompanyId(String name,Long companyId);

  public Optional<CompanyCustomerCategory> findByNameIgnoreCaseAndCompanyId(String name,Long companyId);

  public void deleteByCompanyId(Long companyId);

}
