package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Bin {
    @Id
    String id;
    String location;
    String binNumber;
    private StatusEnum status;
    String companyId;
}
