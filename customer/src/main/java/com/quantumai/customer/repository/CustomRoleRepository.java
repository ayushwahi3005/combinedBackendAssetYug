package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CustomRole;
import java.util.List;
import java.util.Optional;

import com.quantumai.customer.entity.RoleType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomRoleRepository extends MongoRepository<CustomRole, String> , CompanyScopedRepository{

  Optional<CustomRole> findById(String id);

  Optional<CustomRole> findByNameAndCompanyId(String name, Long id);

  Optional<CustomRole> findByTypeAndCompanyId(RoleType type, Long id);

  List<CustomRole> findByCompanyId(Long companyId);

  List<CustomRole> findByCompanyIdAndStatus(Long companyId, String status);

  public void deleteByCompanyId(Long companyId);

}
