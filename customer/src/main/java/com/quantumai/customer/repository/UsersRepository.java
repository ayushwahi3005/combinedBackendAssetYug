package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsersRepository extends MongoRepository<Users, String> {
  public List<Users> findByCompanyId(Long id);

  public Optional<Users> findByCompanyIdAndEmail(Long companyId, String email);

  public Optional<Users> findByEmail(String email);

  //		public Optional<Users> findByNameAndCompanyId(String name,Long companyId);
  public List<Users> findByRoleEndingWithAndCompanyId(String role, Long companyId);
}
