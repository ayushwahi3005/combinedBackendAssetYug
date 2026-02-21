package com.quantumai.customer.service;

import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.RejectedCustomerEmail;
import com.quantumai.customer.entity.TrialStatus;
import com.quantumai.customer.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to handle account deletion logic after trial expiration
 *
 * Workflow:
 * 1. When trial expires: Keep account active for 30 days (grace period)
 * 2. After 30 days: If no subscription, delete account and store email in rejection table
 * 3. On re-signup: Check if email is rejected, skip trial and ask for subscription
 */
@Service
@Slf4j
public class AccountDeletionService {

    @Autowired
    private TrialStatusRepository trialStatusRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RejectedCustomerEmailRepository rejectedCustomerEmailRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Grace period in days after trial expiration
     * Account remains active during this period
     */
    @Value("${app.account.grace-period-days:30}")
    private int gracePeriodDays;

    /**
     * Scheduled task to delete accounts that haven't subscribed after grace period
     * Runs daily at 2 AM
     * Cron format: second minute hour day month weekday
     * "0 0 2 * * *" = 0 seconds, 0 minutes, 2 hours (2 AM), every day, every month, every weekday
     */
//    @Scheduled(cron = "0 0 2 * * *")
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void deleteExpiredAccountsWithoutSubscription() {
        log.info("Starting scheduled task: deleteExpiredAccountsWithoutSubscription");

        try {
            // Find all trial statuses that have expired
            List<TrialStatus> expiredTrials = trialStatusRepository.findByTrialExpiredTrue();

            for (TrialStatus trial : expiredTrials) {
                processExpiredTrial(trial);
            }

            log.info("Completed deleteExpiredAccountsWithoutSubscription task");
        } catch (Exception e) {
            log.error("Error in deleteExpiredAccountsWithoutSubscription", e);
        }
    }

    /**
     * Process an expired trial and delete account if no subscription
     */
    @Transactional
    private void processExpiredTrial(TrialStatus trial) {
        try {
            String email = trial.getCustomerEmail();
            Long companyId = trial.getCompanyId();

            // Check if customer has subscription
            boolean hasSubscription = subscriptionRepository.existsByCompanyId(companyId);

            if (!hasSubscription) {
                // Check if grace period has passed
                LocalDateTime gracePeriodEnd = trial.getTrialEndDate().plusDays(gracePeriodDays);
                LocalDateTime now = LocalDateTime.now();

                if (now.isAfter(gracePeriodEnd)) {
                    // Delete account and store email as rejected
                    deleteAccountAndRejectEmail(email, companyId, trial);
                } else {
                    // Send grace period reminder if close to deletion
                    LocalDateTime reminderDate = gracePeriodEnd.minusDays(3);
                    if (now.isAfter(reminderDate) && now.isBefore(gracePeriodEnd)) {
                        sendGracePeriodReminderEmail(email, gracePeriodEnd);
                    }
                }
            } else {
                // Customer has subscription, mark trial as ended
                trial.setTrialActive(false);
                trial.setTrialExpired(true);
                trialStatusRepository.save(trial);
                log.info("Trial ended but subscription active for email: {}", email);
            }
        } catch (Exception e) {
            log.error("Error processing expired trial for email: {}", trial.getCustomerEmail(), e);
        }
    }

    /**
     * Delete account and reject the email for future signups
     */
    @Transactional
    public void deleteAccountAndRejectEmail(String email, Long companyId, TrialStatus trial) {
        try {
            log.info("Deleting account for email: {} due to no subscription after trial", email);

            // Create rejection record
            RejectedCustomerEmail rejectedEmail = new RejectedCustomerEmail();
            rejectedEmail.setEmail(email);
            rejectedEmail.setCompanyId(companyId);
            rejectedEmail.setReason("NO_SUBSCRIPTION");
            rejectedEmail.setRejectionDate(LocalDateTime.now());
            rejectedEmail.setAccountDeleteDate(LocalDateTime.now());
            rejectedEmail.setOriginalTrialEndDate(trial.getTrialEndDate());
            rejectedEmail.setAccountDeleted(true);
            rejectedEmail.setCreatedAt(LocalDateTime.now());
            rejectedEmail.setUpdatedAt(LocalDateTime.now());

            rejectedCustomerEmailRepository.save(rejectedEmail);

            // Delete customer from system
            Optional<Customer> customer = customerRepository.findByEmailAndCompanyId(email, companyId);
            if (customer.isPresent()) {
                customerRepository.delete(customer.get());
                log.info("Successfully deleted customer account for email: {}", email);
            }

            // Delete trial status
            trialStatusRepository.delete(trial);

            // Send deletion notification email
            sendAccountDeletionNotificationEmail(email);

        } catch (Exception e) {
            log.error("Error deleting account for email: {}", email, e);
        }
    }

    /**
     * Check if email was previously rejected (deleted due to no subscription)
     */
    public boolean isEmailRejected(String email, Long companyId) {
        Optional<RejectedCustomerEmail> rejectedEmail =
            rejectedCustomerEmailRepository.findByEmailAndCompanyId(email, companyId);
        return rejectedEmail.isPresent();
    }

    /**
     * Get rejection details for an email
     */
    public Optional<RejectedCustomerEmail> getRejectionDetails(String email, Long companyId) {
        return rejectedCustomerEmailRepository.findByEmailAndCompanyId(email, companyId);
    }

    /**
     * Send grace period reminder email
     */
    private void sendGracePeriodReminderEmail(String email, LocalDateTime deleteDate) {
        try {
            String message = "Your trial period has ended, and your account will be automatically deleted on " +
                    deleteDate.toLocalDate() + " if you don't subscribe to a plan.\n\n" +
                    "To keep your account and data, please subscribe to a plan today.";

            emailService.sendEmail(email, "Account Deletion Warning - Action Required", message, "Valued Customer");
            log.info("Grace period reminder email sent to: {}", email);
        } catch (Exception e) {
            log.error("Error sending grace period reminder to: {}", email, e);
        }
    }

    /**
     * Send account deletion confirmation email
     */
    private void sendAccountDeletionNotificationEmail(String email) {
        try {
            String message = "Your account has been deleted as no subscription plan was activated within the grace period.\n\n" +
                    "If you wish to use AssetYug again, you can sign up with this email. However, you will not receive a new trial period. " +
                    "You will need to subscribe to a plan to use the application.";

            emailService.sendEmail(email, "Account Deletion Complete", message, "Valued Customer");
            log.info("Account deletion notification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Error sending account deletion notification to: {}", email, e);
        }
    }

    /**
     * Handle immediate account deletion (when user manually deletes data)
     * Keep account active for grace period with "ACCOUNT_DELETED" reason
     */
    @Transactional
    public void markAccountForDeletion(String email, Long companyId) {
        try {
            log.info("Marking account for deletion (grace period) for email: {}", email);

            // Find existing rejection record or create new one
            Optional<RejectedCustomerEmail> existingRecord =
                rejectedCustomerEmailRepository.findByEmailAndCompanyId(email, companyId);

            RejectedCustomerEmail rejectedEmail = existingRecord.orElseGet(() ->
                new RejectedCustomerEmail(email, companyId, "ACCOUNT_DELETED"));

            rejectedEmail.setReason("ACCOUNT_DELETED");
            rejectedEmail.setRejectionDate(LocalDateTime.now());
            rejectedEmail.setAccountDeleteDate(LocalDateTime.now().plusDays(gracePeriodDays));
            rejectedEmail.setAccountDeleted(false);
            rejectedEmail.setUpdatedAt(LocalDateTime.now());

            rejectedCustomerEmailRepository.save(rejectedEmail);

            // Send grace period email
            sendGracePeriodNotificationForDataDeletion(email, rejectedEmail.getAccountDeleteDate());

        } catch (Exception e) {
            log.error("Error marking account for deletion for email: {}", email, e);
        }
    }

    /**
     * Send notification about grace period after data deletion
     */
    private void sendGracePeriodNotificationForDataDeletion(String email, LocalDateTime deleteDate) {
        try {
            String message = "Your data has been deleted from our system. Your account will remain active for the next " +
                    gracePeriodDays + " days (until " + deleteDate.toLocalDate() + ").\n\n" +
                    "During this grace period, you can choose to subscribe to a plan to restore access and continue using the application.\n\n" +
                    "After the grace period expires, your account will be permanently deleted if no subscription is activated.";

            emailService.sendEmail(email, "Grace Period Notification - Data Deleted", message, "Valued Customer");
            log.info("Grace period notification sent to: {}", email);
        } catch (Exception e) {
            log.error("Error sending grace period notification to: {}", email, e);
        }
    }
}

