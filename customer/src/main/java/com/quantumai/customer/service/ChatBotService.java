package com.quantumai.customer.service;

import com.quantumai.customer.dto.ChatBotRequestDTO;
import com.quantumai.customer.dto.ChatBotResponseDTO;

public interface ChatBotService {
    ChatBotResponseDTO processQuery(ChatBotRequestDTO request);
}
