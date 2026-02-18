package com.quantumai.customer.entity.IdGenerator;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class BinIdGenerator {

    @Id
    private String id;
    private Long seq;
    private Long companyId;
}
