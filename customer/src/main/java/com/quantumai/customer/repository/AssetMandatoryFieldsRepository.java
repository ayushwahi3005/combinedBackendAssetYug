package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetMandatoryFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetMandatoryFieldsRepository extends MongoRepository<AssetMandatoryFields,String> {
	public Optional<AssetMandatoryFields> findByNameAndCompanyId(String name,String companyId);
	public List<AssetMandatoryFields> findByCompanyId(String companyId);

}
