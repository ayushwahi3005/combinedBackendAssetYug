package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> , CompanyScopedRepository{

  Boolean existsByEmail(String email);

  Optional<Customer> findByEmail(String email);

  List<Customer> findByCompanyId(Long companyId);


  Optional<Customer> findByEmailAndCompanyId(String email, Long companyId);

  Long countByRoleAndCompanyId(String role, Long id);
  List<Customer> findByRoleAndCompanyId(String role, Long id);

  public void deleteByCompanyId(Long companyId);
  

}
