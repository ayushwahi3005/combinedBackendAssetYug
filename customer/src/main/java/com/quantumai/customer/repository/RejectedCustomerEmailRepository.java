package com.quantumai.customer.repository;

import com.quantumai.customer.entity.RejectedCustomerEmail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RejectedCustomerEmailRepository extends MongoRepository<RejectedCustomerEmail, String>, CompanyScopedRepository {

    /**
     * Find a rejected customer email record
     */
    Optional<RejectedCustomerEmail> findByEmailAndCompanyId(String email, Long companyId);

    /**
     * Find all rejected emails for a company
     */
    List<RejectedCustomerEmail> findByCompanyId(Long companyId);

    /**
     * Find rejected customers whose accounts should be deleted (passed the grace period)
     */
    List<RejectedCustomerEmail> findByIsAccountDeletedFalseAndAccountDeleteDateBefore(LocalDateTime dateTime);

    /**
     * Find rejected customers by reason
     */
    List<RejectedCustomerEmail> findByReasonAndCompanyId(String reason, Long companyId);

    /**
     * Check if email has been rejected before
     */
    boolean existsByEmailAndCompanyId(String email, Long companyId);

    /**
     * Delete records by company ID (for cleanup)
     */
    void deleteByCompanyId(Long companyId);
}

