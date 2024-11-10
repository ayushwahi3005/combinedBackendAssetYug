package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
@Data
@Document
public class Subscription {

    @Id
    private String id;
    private String companyId;
    private SubscriptionEnum status;
    private String plan;
    private Integer person;
    private LocalDate subscriptionDate;
    private LocalDate expiryDate;
    private SubscriptionPlan subscriptionPlan;
}
