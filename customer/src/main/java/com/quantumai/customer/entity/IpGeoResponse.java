package com.quantumai.customer.entity;

import lombok.Data;

@Data
public class IpGeoResponse {
    private String country;
    private String regionName;
    private String city;
    private String lat;
    private String lon;
}