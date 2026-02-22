package com.quantumai.customer.dto;

import lombok.Data;

import java.time.LocalDate;
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

}
