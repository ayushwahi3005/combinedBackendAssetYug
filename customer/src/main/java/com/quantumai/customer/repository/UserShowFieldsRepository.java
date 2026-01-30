package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserShowFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserShowFieldsRepository extends MongoRepository<UserShowFields, String> , CompanyScopedRepository{
  public Optional<UserShowFields> findByNameIgnoreCaseAndCompanyId(String name, Long companyId);

  public List<UserShowFields> findByCompanyId(Long companyId);

  public void deleteByCompanyId(Long companyId);
}
