package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Admin;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdminRepository extends MongoRepository<Admin, String> {

  Optional<Admin> findByEmail(String email);

//  public void deleteByCompanyId(Long companyId);
}
