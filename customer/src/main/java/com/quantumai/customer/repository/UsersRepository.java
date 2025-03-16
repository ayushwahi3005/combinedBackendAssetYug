package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsersRepository extends MongoRepository<Users, String> {
  public List<Users> findByCompanyId(String id);

  public Optional<Users> findByCompanyIdAndEmail(String companyId, String email);

  public Optional<Users> findByEmail(String email);

  //		public Optional<Users> findByNameAndCompanyId(String name,String companyId);
  public List<Users> findByRoleEndingWithAndCompanyId(String role, String companyId);
}
