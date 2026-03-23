package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatBotResponseDTO {
    private String answerText;
    private Object data;
    private boolean success;
    private String entity;
}
