package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class ChatBotRequestDTO {
    private String query;
    private Long companyId;
}
