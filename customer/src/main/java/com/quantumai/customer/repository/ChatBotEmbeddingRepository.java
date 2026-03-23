package com.quantumai.customer.repository;

import com.quantumai.customer.entity.ChatBotEmbedding;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatBotEmbeddingRepository extends MongoRepository<ChatBotEmbedding, String> {
    List<ChatBotEmbedding> findByCompanyId(Long companyId);

    /** Global intents have companyId = 0 */
    List<ChatBotEmbedding> findByCompanyIdIn(List<Long> companyIds);

    void deleteByCompanyId(Long companyId);
}
