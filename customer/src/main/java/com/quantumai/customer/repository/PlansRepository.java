package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Plans;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlansRepository extends MongoRepository<Plans, String>{
  List<Plans> findAll();

  Optional<Plans> getById(String id);

//  public void deleteByCompanyId(Long companyId);
}
