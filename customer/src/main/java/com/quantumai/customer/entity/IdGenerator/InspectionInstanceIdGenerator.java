package com.quantumai.customer.entity.IdGenerator;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class InspectionInstanceIdGenerator {

    @Id
    private String id;
    private Long seq;

    @Indexed(unique = true)
    private Long companyId;
}
