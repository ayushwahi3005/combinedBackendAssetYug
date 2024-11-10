package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetIdTable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AssetIdTableRepository extends MongoRepository<AssetIdTable,String> {
	public Optional<AssetIdTable> findByCompanyId(String id);
}
