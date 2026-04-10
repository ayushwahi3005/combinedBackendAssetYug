package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for deleting accounts that have not subscribed
 * after their trial expired and the grace period (account.deletion.grace.days) has passed.
 *
 * Flow:
 * 1. Trial ends (7 days) - PurgeService deletes company data after purge.expiry.days (keeps account active)
 * 2. Account stays active for account.deletion.grace.days (30 days) after trial end
 * 3. After grace period, if no subscription - delete account and store email in BlacklistedEmails
 * 4. If user signs up again with blacklisted email - no trial, must subscribe immediately
 */
@Service
@Slf4j
public class AccountDeletionService {

    @Autowired
    private TrialStatusRepository trialStatusRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BlacklistedEmailRepository blacklistedEmailRepository;

    @Autowired
    private EmailService emailService;

    @Value("${account.deletion.grace.days:30}")
    private int graceDays;

    /**
     * Runs daily at 2 AM to check for expired trial accounts
     * that have passed the grace period without subscribing.
     * Spring @Scheduled cron requires 6 fields: sec min hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * ?")
//    @Scheduled(cron = "0 * * * * ?")
    public void deleteExpiredAccountsWithoutSubscription() {
        log.info("Account deletion job started at {}", LocalDateTime.now());

        // Find trials that ended more than graceDays ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(graceDays);

        List<TrialStatus> expiredTrials = trialStatusRepository.findByTrialEndDateBefore(cutoffDate);

        for (TrialStatus trial : expiredTrials) {
            Long companyId = trial.getCompanyId();
            String email = trial.getCustomerEmail();

            // Check if they have an active subscription
            Optional<Subscription> activeSubscription =
                    subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);

            if (activeSubscription.isEmpty()) {
                log.info("Deleting account for email: {}, companyId: {} - no subscription after grace period", email, companyId);
                deleteAccountAndBlacklist(email, companyId);
            }
        }

        log.info("Account deletion job completed at {}", LocalDateTime.now());
    }

    /**
     * Deletes the customer account, user records, and stores email in blacklist.
     */
    private void deleteAccountAndBlacklist(String email, Long companyId) {
        try {
            // Send final notification email before deletion
            sendAccountDeletionEmail(email, companyId);

            // Delete all users of this company from Firebase, then from DB
            List<Users> companyUsers = usersRepository.findByCompanyId(companyId);
            for (Users user : companyUsers) {
                deleteFirebaseUser(user.getEmail());
            }
            usersRepository.deleteByCompanyId(companyId);

            // Blacklist and delete all customer (admin) accounts for this company from Firebase + DB
            List<Customer> companyCustomers = customerRepository.findByCompanyId(companyId);
            for (Customer c : companyCustomers) {
                deleteFirebaseUser(c.getEmail());
                if (!blacklistedEmailRepository.existsByEmail(c.getEmail())) {
                    BlacklistedEmail bl = new BlacklistedEmail(
                            c.getEmail(),
                            companyId,
                            "Account deleted - associated company trial expired without subscription"
                    );
                    blacklistedEmailRepository.save(bl);
                }
                customerRepository.delete(c);
            }

            // Also handle the primary email if not already covered above
            if (!blacklistedEmailRepository.existsByEmail(email)) {
                BlacklistedEmail blacklistedEmail = new BlacklistedEmail(
                        email,
                        companyId,
                        "Account deleted after trial expiration without subscription"
                );
                blacklistedEmailRepository.save(blacklistedEmail);
                log.info("Email blacklisted: {}", email);
            }

            // Delete trial status
            trialStatusRepository.deleteByCompanyId(companyId);

            log.info("Account fully deleted for companyId: {}", companyId);

        } catch (Exception e) {
            log.error("Error deleting account for email: {}, companyId: {}", email, companyId, e);
        }
    }

    /**
     * Deletes a user from Firebase Authentication by email.
     * Silently skips if the user is not found in Firebase.
     */
    private void deleteFirebaseUser(String email) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
            log.info("Firebase user deleted: {}", email);
        } catch (Exception e) {
            log.info("Firebase user not found or could not be deleted for email: {} - {}", email, e.getMessage());
        }
    }

    private void sendAccountDeletionEmail(String email, Long companyId) {
        try {
            emailService.sendEmail(
                    email,
                    "AssetYug - Account Deleted",
                    "Your account has been permanently deleted because your free trial expired and no subscription was activated within the grace period.\n\n" +
                            "If you wish to use AssetYug again, please sign up and subscribe to a plan. " +
                            "Please note that a free trial will not be available for returning users.\n\n" +
                            "If you have any questions, feel free to reach out to our support team.",
                    "User"
            );
        } catch (Exception e) {
            log.error("Failed to send account deletion email to: {}", email, e);
        }
    }

    /**
     * Checks if an email is blacklisted (previously had trial and account was deleted).
     */
    public boolean isEmailBlacklisted(String email) {
        return blacklistedEmailRepository.existsByEmail(email);
    }
}
