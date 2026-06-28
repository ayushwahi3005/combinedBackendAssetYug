package com.quantumai.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AssetCheckInOutData {

    private Integer assetId;
    private String assetName;
    private String customerId;
    private String customerName;
    private String action;
    private LocalDateTime date;
    private LocalTime time;
    private String location;
    private String username;
    private String assetStatus;

    /** Used only for aggregation mapping; not returned in API response. */
    @JsonIgnore
    private LocalDateTime updateTime;

    /** Used only for aggregation mapping; not returned in API response. */
    @JsonIgnore
    private Integer companyCustomerId;

}
