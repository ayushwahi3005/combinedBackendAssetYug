package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Document
public class Payment {

    @Id
    private String id;


    private BigDecimal amount;


    private String currency;


    private PaymentStatus paymentStatus;


    private String cardholderName;


    private LocalDateTime transactionDate;


    private PaymentType paymentType;  // e.g., CREDIT_CARD, DEBIT_CARD


    private String description;

    private String companyId;
}
