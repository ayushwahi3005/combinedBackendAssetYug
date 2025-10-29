package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.StatusEnum;
import com.quantumai.customer.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UsersRepository extends MongoRepository<Users, String> {
  public List<Users> findByCompanyId(Long id);

  public Optional<Users> findByCompanyIdAndEmail(Long companyId, String email);

  public Optional<Users> findByEmail(String email);

  //		public Optional<Users> findByNameAndCompanyId(String name,Long companyId);
  public List<Users> findByRoleEndingWithAndCompanyId(String role, Long companyId);

    List<Users> findByCompanyIdAndStatus(Long companyId, StatusEnum status);

//    List<Users> findByCompanyIdAndCustomRole(Long companyId, StatusEnum status);

//  @Query("{ 'role.name': ?0, 'companyId': ?1 }")
//  List<Users> findUsersByRoleNameAndCompanyId(String roleName, Long companyId);


  long countByCompanyIdAndStatus(Long companyId, StatusEnum status);
  
  Optional<Customer> findByIdAndCompanyId(String id, Long companyId);
}
