package com.quantumai.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Stores embedded vector representations of intent patterns
 * for semantic similarity matching in chatbot queries.
 */
@Data
@Document(collection = "chatbot_embeddings")
@AllArgsConstructor
@NoArgsConstructor
public class ChatBotEmbedding {

    @Id
    private String id;

    @Indexed
    private Long companyId;

    /** The intent key, e.g., "COUNT_ASSETS", "LIST_CUSTOMERS", "FIND_ASSET_BY_NAME" */
    private String intentKey;

    /** The original phrase that was embedded */
    private String phrase;

    /** The entity this intent targets: ASSET, CUSTOMER, USER, CATEGORY, LOCATION, INSPECTION, CHECK_IN_OUT */
    private String entity;

    /** The operation type: COUNT, LIST, FIND, SUMMARY */
    private String operation;

    /** The embedding vector from OpenAI */
    private List<Double> embedding;

  private String createdBy;
  private String lastUpdatedBy;
}
