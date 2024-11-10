package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetQR;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;



public interface AssetQRRepository extends MongoRepository<AssetQR,String>{
	public Optional<AssetQR> findByCompanyId(String companyId);
}
