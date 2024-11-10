package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetFile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssetFileRepository extends MongoRepository<AssetFile,String> {
	public List<AssetFile> findByAssetId(String assetId);
}
