package com.quantumai.customer.repository;

import com.quantumai.customer.entity.TrialStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrialStatusRepository extends MongoRepository<TrialStatus, String> , CompanyScopedRepository{
    Optional<TrialStatus> findByCustomerEmail(String customerEmail);
    Optional<TrialStatus> findByCompanyId(Long companyId);
    List<TrialStatus> findByIsTrialActiveTrue();
    List<TrialStatus> findByTrialExpiredTrue();
    List<TrialStatus> findByTrialEndDateBeforeAndIsTrialActiveTrueAndTrialExpiredFalse(LocalDateTime currentTime);
    List<TrialStatus> findByTrialEndDateBetweenAndIsTrialActiveTrueAndTrialExpirationNotificationSentFalse(
            LocalDateTime startTime, LocalDateTime endTime);
    List<TrialStatus> findByTrialEndDateBetweenAndIsTrialActiveTrueAndFinalWarningNotificationSentFalse(
            LocalDateTime startTime, LocalDateTime endTime);

    List<TrialStatus> findByTrialEndDateBefore(LocalDateTime endTime);

    List<TrialStatus> findByTrialEndDateBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );


    public void deleteByCompanyId(Long companyId);
}
