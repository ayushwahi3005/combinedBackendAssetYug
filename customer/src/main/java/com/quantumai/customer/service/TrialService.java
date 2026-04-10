package com.quantumai.customer.service;

import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.entity.TrialStatus;
import com.quantumai.customer.repository.BlacklistedEmailRepository;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.TrialStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TrialService {

    @Autowired
    private TrialStatusRepository trialStatusRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BlacklistedEmailRepository blacklistedEmailRepository;

    /**
     * Initialize trial for a new customer.
     * If the email is blacklisted (previously had a trial and account was deleted),
     * no trial is granted — user must subscribe to a plan.
     */
    int trial_period=7;
    public void initializeTrial(String customerEmail, Long companyId) {
        // Check if user previously had a trial and was blacklisted
        if (blacklistedEmailRepository.existsByEmail(customerEmail)) {
            // Do not grant trial — user must subscribe
            return;
        }

        Optional<TrialStatus> existingTrial = trialStatusRepository.findByCustomerEmail(customerEmail);
        
        if (existingTrial.isEmpty()) {
            TrialStatus trialStatus = new TrialStatus();
            trialStatus.setCustomerEmail(customerEmail);
            trialStatus.setCompanyId(companyId);
            trialStatus.setTrialStartDate(LocalDateTime.now());
            trialStatus.setTrialEndDate(LocalDateTime.now().plusDays(trial_period));
            trialStatus.setTrialActive(true);
            trialStatus.setTrialExpired(false);
            trialStatus.setTrialExpirationNotificationSent(false);
            trialStatus.setFinalWarningNotificationSent(false);
            
            trialStatusRepository.save(trialStatus);
            
            // Update customer entity as well
            Optional<Customer> customer = customerRepository.findByEmail(customerEmail);
            if (customer.isPresent()) {
                Customer c = customer.get();
                c.setTrialStartDate(LocalDateTime.now());
                c.setTrialEndDate(LocalDateTime.now().plusDays(trial_period));
                c.setTrialActive(true);
                c.setTrialExpired(false);
                c.setTrialExpirationNotificationSent(false);
                customerRepository.save(c);
            }
        }
    }

    /**
     * Check if customer's trial is active and not expired
     */
    public boolean isTrialActive(String customerEmail) {
        Optional<TrialStatus> trialStatus = trialStatusRepository.findByCustomerEmail(customerEmail);
        if (trialStatus.isPresent()) {
            TrialStatus trial = trialStatus.get();
            return trial.isTrialActive() && !trial.isTrialExpired() && 
                   LocalDateTime.now().isBefore(trial.getTrialEndDate());
        }
        return false;
    }

    /**
     * Check if customer's trial has expired
     */
    public boolean isTrialExpired(String customerEmail) {
        Optional<TrialStatus> trialStatus = trialStatusRepository.findByCustomerEmail(customerEmail);
        if (trialStatus.isPresent()) {
            TrialStatus trial = trialStatus.get();
            return trial.isTrialExpired() || LocalDateTime.now().isAfter(trial.getTrialEndDate());
        }
        return false;
    }

    /**
     * Get trial status for a customer
     */
    public Optional<TrialStatus> getTrialStatus(String customerEmail) {
        return trialStatusRepository.findByCustomerEmail(customerEmail);
    }

    /**
     * Scheduled task to check trial expiration and send notifications
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void checkTrialExpirationAndNotify() {
        LocalDateTime now = LocalDateTime.now();
        
        // Check for trials that have expired
        List<TrialStatus> expiredTrials = trialStatusRepository
            .findByTrialEndDateBeforeAndIsTrialActiveTrueAndTrialExpiredFalse(now);
        
        for (TrialStatus trial : expiredTrials) {
            expireTrialAndNotify(trial);
        }
        
        // Check for trials expiring in 3 days (first warning)
        LocalDateTime threeDaysFromNow = now.plusDays(3);
        List<TrialStatus> trialsExpiringIn3Days = trialStatusRepository
            .findByTrialEndDateBetweenAndIsTrialActiveTrueAndTrialExpirationNotificationSentFalse(
                now, threeDaysFromNow);
        
        for (TrialStatus trial : trialsExpiringIn3Days) {
            sendTrialExpirationWarning(trial, 3);
        }
        
        // Check for trials expiring in 1 day (final warning)
        LocalDateTime oneDayFromNow = now.plusDays(1);
        List<TrialStatus> trialsExpiringIn1Day = trialStatusRepository
            .findByTrialEndDateBetweenAndIsTrialActiveTrueAndFinalWarningNotificationSentFalse(
                now, oneDayFromNow);
        
        for (TrialStatus trial : trialsExpiringIn1Day) {
            sendTrialExpirationWarning(trial, 1);
        }
    }

    /**
     * Expire trial and send notification
     */
    private void expireTrialAndNotify(TrialStatus trial) {
        // Update trial status
        trial.setTrialExpired(true);
        trial.setTrialActive(false);
        trial.updateTimestamp();
        trialStatusRepository.save(trial);
        
        // Update customer entity
        Optional<Customer> customer = customerRepository.findByEmail(trial.getCustomerEmail());
        if (customer.isPresent()) {
            Customer c = customer.get();
            c.setTrialExpired(true);
            c.setTrialActive(false);
            customerRepository.save(c);
        }
        
        // Send expiration notification
        Notification notification = new Notification();
        notification.setTitle("Free Trial Expired");
        notification.setMessage("Your 7-day free trial has expired. Please upgrade to a paid plan to continue using our services.");
        notification.setAlertType("TRIAL_EXPIRED");
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationService.sendNotificationToUser(trial.getCustomerEmail(), notification);
    }

    /**
     * Send trial expiration warning
     */
    private void sendTrialExpirationWarning(TrialStatus trial, int daysRemaining) {
        Notification notification = new Notification();
        notification.setTitle("Free Trial Ending Soon");
        notification.setMessage(String.format("Your free trial will expire in %d day(s). Please upgrade to continue using our services.", daysRemaining));
        notification.setAlertType("TRIAL_WARNING");
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationService.sendNotificationToUser(trial.getCustomerEmail(), notification);
        
        // Update notification flags
        if (daysRemaining == 3) {
            trial.setTrialExpirationNotificationSent(true);
        } else if (daysRemaining == 1) {
            trial.setFinalWarningNotificationSent(true);
        }
        trial.updateTimestamp();
        trialStatusRepository.save(trial);
    }

    /**
     * Activate paid subscription and deactivate trial
     */
    public void activatePaidSubscription(String customerEmail) {
        Optional<TrialStatus> trialStatus = trialStatusRepository.findByCustomerEmail(customerEmail);
        if (trialStatus.isPresent()) {
            TrialStatus trial = trialStatus.get();
            trial.setTrialActive(false);
            trial.updateTimestamp();
            trialStatusRepository.save(trial);
            
            // Update customer entity
            Optional<Customer> customer = customerRepository.findByEmail(customerEmail);
            if (customer.isPresent()) {
                Customer c = customer.get();
                c.setTrialActive(false);
                customerRepository.save(c);
            }
        }
    }

    public TrialStatus getTrialDetails(Long companyId){
        Optional<TrialStatus> trialStatusOptional=trialStatusRepository.findByCompanyId(companyId);
        return trialStatusOptional.orElse(null);
    }

    /**
     * Check if user is eligible for a free trial.
     * Returns false if the email is blacklisted (previously had trial and account was deleted).
     */
    public boolean isEligibleForTrial(String email) {
        return !blacklistedEmailRepository.existsByEmail(email);
    }
}
