package com.quantumai.customer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssetCountByCompanyCustomerDTO {
    private String companyCustomerId;
    private String companyCustomerName;
    private String email;
    private Long assetCount;

    public AssetCountByCompanyCustomerDTO(String companyCustomerId, String companyCustomerName, String email, Long assetCount) {
        this.companyCustomerId = companyCustomerId;
        this.companyCustomerName = companyCustomerName;
        this.email = email;
        this.assetCount = assetCount;
    }
}
