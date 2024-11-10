package com.quantumai.customer.repository;


import com.quantumai.customer.dto.AssetsDTO;
import com.quantumai.customer.entity.Assets;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssetsRepository extends MongoRepository<Assets,String> {
	List<Assets> findByCompanyId(String companyId);
	List<Assets> findByCustomerId(String customerId);
	Assets findByAssetIdAndCompanyId(Integer assetId,String companyId);
	List<AssetsDTO> findByCompanyIdAndSerialNumber(String companyId, String serialNumber);
}
