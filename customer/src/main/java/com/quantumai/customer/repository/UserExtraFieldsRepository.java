package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserExtraFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserExtraFieldsRepository extends MongoRepository<UserExtraFields, String> {
  public List<UserExtraFields> findByUserId(String userId);

  public List<UserExtraFields> findByCompanyId(Long companyId);

  public List<UserExtraFields> findByNameIgnoreCase(String name);

  public Optional<UserExtraFields> findByNameIgnoreCaseAndUserId(String name, String userId);
}
