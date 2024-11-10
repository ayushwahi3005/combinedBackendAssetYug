package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCheckInOut;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssetCheckInOutRepository extends MongoRepository<AssetCheckInOut,String> {
			public List<AssetCheckInOut> findByAssetId(String assetid);
			public List<AssetCheckInOut> findByCompanyId(String companyId);
}
