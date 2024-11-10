package com.quantumai.customer.repository;


import com.quantumai.customer.entity.Admin;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdminRepository extends MongoRepository<Admin,String> {

    Optional<Admin> findByEmail(String email);
}
