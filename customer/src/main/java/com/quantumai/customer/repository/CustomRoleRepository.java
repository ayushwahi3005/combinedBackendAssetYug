package com.quantumai.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;


import com.quantumai.customer.entity.CustomRole;


public interface CustomRoleRepository extends MongoRepository<CustomRole,String> {
	
	
	Optional<CustomRole> findById(String id);
	Optional<CustomRole> findByNameAndCompanyId(String name,String id);
	List<CustomRole> findByCompanyId(String companyId);
	
	

}
