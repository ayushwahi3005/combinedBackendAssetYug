package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCategory;


import com.quantumai.customer.entity.CompanyCustomerCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetCategoryRepository extends MongoRepository<AssetCategory, String> {
    public List<AssetCategory> findByCompanyId(String companyId);
    public Optional<AssetCategory> findByName(String name);
    public List<AssetCategory> findByCompanyIdAndStatus(String companyId, String status);

}
