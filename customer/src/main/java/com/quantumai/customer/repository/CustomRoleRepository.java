package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CustomRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomRoleRepository extends MongoRepository<CustomRole, String> {

  Optional<CustomRole> findById(String id);

  Optional<CustomRole> findByNameAndCompanyId(String name, Long id);

  List<CustomRole> findByCompanyId(Long companyId);

  List<CustomRole> findByCompanyIdAndStatus(Long companyId, String status);
}
