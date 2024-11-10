package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetExtraFieldName;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssetExtraFieldNameRepository extends MongoRepository<AssetExtraFieldName,String>{
	public AssetExtraFieldName findByNameAndCompanyId(String name,String companyId);
	public List<AssetExtraFieldName> findByCompanyId(String companyId);
}
