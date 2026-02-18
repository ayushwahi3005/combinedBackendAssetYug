package com.quantumai.customer.dto;

import com.quantumai.customer.entity.AssetCheckInOut;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetWithCustomFieldsDTO {
    private String id;
    private Integer assetId;
    private String name;
    private String serialNumber;
    private String category;
    private String customer;
    private String customerId;
    private String location;
    private String locationName;
    private String status;
    private String email;
    private String image;
    private Long companyId;
    private String updatedAt;
    private String checkedInOutStatus;
    private AssetCheckInOut assetCheckInOut;
    private Map<String, String> customFields;
}

