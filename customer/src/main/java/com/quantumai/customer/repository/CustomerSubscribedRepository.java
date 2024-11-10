package com.quantumai.customer.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;


import com.quantumai.customer.entity.CustomerSubscribed;

public interface CustomerSubscribedRepository extends MongoRepository<CustomerSubscribed,String>{
	
	Optional<CustomerSubscribed> findById(String email);

}
