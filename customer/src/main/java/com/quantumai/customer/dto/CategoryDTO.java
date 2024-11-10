package com.quantumai.customer.dto;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
public class CategoryDTO {

    private String id;
    private String name;
    private String status;
    private String companyId;
}
