package com.quantumai.customer.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.quantumai.customer.entity.CompanyInformation;

public interface CompanyInformationRepository extends MongoRepository<CompanyInformation,String> {
	Optional<CompanyInformation> findByCustomerEmail(String email);
	
	
}
