package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetShowFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetShowFieldsRepository extends MongoRepository<AssetShowFields,String> {
	public Optional<AssetShowFields> findByNameAndCompanyId(String name,String companyId);
	public List<AssetShowFields> findByCompanyId(String companyId);
}
