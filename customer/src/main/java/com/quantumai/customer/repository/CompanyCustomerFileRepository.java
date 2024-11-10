package com.quantumai.customer.repository;


import com.quantumai.customer.entity.CompanyCustomerFile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CompanyCustomerFileRepository  extends MongoRepository<CompanyCustomerFile,String> {
	public List<CompanyCustomerFile> findByCompanyCustomerId(String assetId);

}
