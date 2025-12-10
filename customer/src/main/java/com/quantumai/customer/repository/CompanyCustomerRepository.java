package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CompanyCustomerRepository extends MongoRepository<CompanyCustomer, String> {

  public List<CompanyCustomer> findByCompanyId(Long id);

  public Optional<CompanyCustomer> findByCompanyCustomerId(Integer id);

  public Optional<CompanyCustomer> findByEmailAndCompanyId(String email,Long companyId);



  public CompanyCustomer findByCompanyCustomerIdAndCompanyId(
      Integer companyCustomerid, Long companyId);

  public int countByCategory(String category);
}
