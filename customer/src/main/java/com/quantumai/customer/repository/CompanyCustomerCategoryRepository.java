package com.quantumai.customer.repository;


import com.quantumai.customer.entity.CompanyCustomerCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyCustomerCategoryRepository extends MongoRepository<CompanyCustomerCategory, String> {
    public List<CompanyCustomerCategory> findByCompanyId(String companyId);
    public List<CompanyCustomerCategory> findByCompanyIdAndStatus(String companyId,String status);

    public Optional<CompanyCustomerCategory> findByName(String name);
}
