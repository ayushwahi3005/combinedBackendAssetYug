package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetExtraFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetExtraFieldsRepository extends MongoRepository<AssetExtraFields,String> {
	public List<AssetExtraFields>findByAssetId(String assetId);
	public List<AssetExtraFields> findByCompanyId(String companyId);
	public List<AssetExtraFields> findByName(String name);
	public Optional<AssetExtraFields> findByNameAndAssetId(String name,String assetId);

}
