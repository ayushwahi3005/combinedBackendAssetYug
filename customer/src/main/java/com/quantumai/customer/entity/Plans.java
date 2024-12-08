package com.quantumai.customer.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Plans {

    @Id
    private String id;
    private String name;
    private Double monthly;
    private Double monthlyDiscount;
    private Double yearly;
    private Double yearlyDiscount;
    private String cardColor;
    private String description1;
    private String description2;
    private String description3;
    private String description4;
    private String description5;


}
