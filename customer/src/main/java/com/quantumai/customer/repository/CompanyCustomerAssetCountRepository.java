package com.quantumai.customer.repository;

import com.quantumai.customer.dto.AssetCountByCompanyCustomerDTO;
import java.util.List;

public interface CompanyCustomerAssetCountRepository {

    /**
     * Get asset count by company customer with sorting
     * @param companyId the company ID to filter by
     * @param sortOrder "ASC" for ascending, "DESC" for descending
     * @return List of AssetCountByCompanyCustomerDTO sorted by asset count
     */
    List<AssetCountByCompanyCustomerDTO> getAssetCountByCompanyCustomer(Long companyId, String sortOrder);
}
