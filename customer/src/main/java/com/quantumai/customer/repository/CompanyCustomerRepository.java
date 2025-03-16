package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerRepository extends MongoRepository<CompanyCustomer, String> {

  public List<CompanyCustomer> findByCompanyId(String id);

  public Optional<CompanyCustomer> findByCompanyCustomerId(Integer id);

  public CompanyCustomer findByCompanyCustomerIdAndCompanyId(
      Integer companyCustomerid, String companyId);

  public int countByCategory(String category);
}
