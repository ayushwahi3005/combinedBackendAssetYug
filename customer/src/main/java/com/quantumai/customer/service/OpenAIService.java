package com.quantumai.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles communication with OpenAI API for embeddings and chat completions.
 */
@Service
@Slf4j
public class OpenAIService {

    @Value("${chatbot.openai.api-key:}")
    private String apiKey;

    @Value("${chatbot.openai.model:gpt-4o-mini}")
    private String chatModel;

    @Value("${chatbot.openai.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${chatbot.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    /**
     * Get embedding vector for a given text.
     */
    public List<Double> getEmbedding(String text) {
        if (!isConfigured()) {
            log.warn("OpenAI API key not configured, skipping embedding");
            return new ArrayList<>();
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", embeddingModel,
                    "input", text
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/embeddings", entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingArray = root.path("data").get(0).path("embedding");
            List<Double> embedding = new ArrayList<>();
            for (JsonNode val : embeddingArray) {
                embedding.add(val.asDouble());
            }
            return embedding;
        } catch (Exception e) {
            log.error("Failed to get embedding from OpenAI: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Send a chat completion request to OpenAI with a system prompt and user message.
     */
    public String chatCompletion(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            log.warn("OpenAI API key not configured, skipping chat completion");
            return null;
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", chatModel,
                    "temperature", 0.1,
                    "max_tokens", 2000,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions", entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Failed to get chat completion from OpenAI: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse an AI response as JSON.
     */
    public JsonNode parseJson(String jsonString) {
        try {
            String cleaned = jsonString.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            return null;
        }
    }
}
