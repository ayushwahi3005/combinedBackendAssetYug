package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerFile;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerFileRepository
    extends MongoRepository<CompanyCustomerFile, String> , CompanyScopedRepository{
  public List<CompanyCustomerFile> findByCompanyCustomerId(String assetId);

  Page<CompanyCustomerFile> findByCompanyCustomerId(String companyCustomerId, Pageable pageable);

  public void deleteByCompanyId(Long companyId);
}
