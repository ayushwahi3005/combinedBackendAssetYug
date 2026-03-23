package com.quantumai.customer.controller;

import com.quantumai.customer.dto.ChatBotRequestDTO;
import com.quantumai.customer.dto.ChatBotResponseDTO;
import com.quantumai.customer.service.ChatBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@Slf4j
public class ChatBotAPI {

    @Autowired
    private ChatBotService chatBotService;

    /**
     * Main chatbot endpoint.
     * Accepts a natural language query and returns structured data + AI-generated answer.
     */
    @PostMapping("/query")
    @PreAuthorize("@appSecurity.canView(authentication, #request.companyId, 'assets')")
    public ResponseEntity<ChatBotResponseDTO> query(@RequestBody ChatBotRequestDTO request) {
        log.info("ChatBot query received: companyId={}, query={}", request.getCompanyId(), request.getQuery());

        if (request.getCompanyId() == null) {
            return ResponseEntity.badRequest()
                    .body(new ChatBotResponseDTO("Company ID is required.", null, false, null));
        }

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ChatBotResponseDTO("Please provide a query.", null, false, null));
        }

        try {
            ChatBotResponseDTO response = chatBotService.processQuery(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("ChatBot error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ChatBotResponseDTO("An error occurred while processing your query.", null, false, null));
        }
    }

    /**
     * Health check / capability endpoint.
     */
    @GetMapping("/capabilities")
    public ResponseEntity<?> capabilities() {
        return ResponseEntity.ok(java.util.Map.of(
                "description", "AI-powered data assistant for your asset management platform",
                "supported_entities", java.util.List.of(
                        "Assets", "Customers", "Users",
                        "Asset Categories", "Customer Categories",
                        "Locations", "Bins", "Inspections", "Check-In/Out"
                ),
                "supported_operations", java.util.List.of(
                        "Count (e.g., 'how many assets?')",
                        "List (e.g., 'show all customers')",
                        "Find (e.g., 'find asset named Laptop-1')",
                        "Summary (e.g., 'give me a dashboard summary')"
                ),
                "example_queries", java.util.List.of(
                        "How many assets do I have?",
                        "List all active customers",
                        "Find user with email john@example.com",
                        "Show all asset categories",
                        "How many assets are checked out?",
                        "Give me a summary of everything",
                        "Find assets in category Electronics",
                        "Show all locations",
                        "Count inactive users"
                ),
                "ai_powered", true
        ));
    }
}
