package com.quantumai.customer.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.quantumai.customer.entity.AccountLockInfo;

public interface AccountLockInfoRepository extends MongoRepository<AccountLockInfo,String> {
	
	public Optional<AccountLockInfo> findByCustomerEmail(String email);

}
