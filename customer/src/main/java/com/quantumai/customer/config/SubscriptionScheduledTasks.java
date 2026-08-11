package com.quantumai.customer.config;

import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.entity.NotificationType;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.TrialStatusRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.service.EmailService;
import com.quantumai.customer.service.NotificationService;
import com.quantumai.customer.service.PurgeService;
import com.quantumai.customer.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled subscription and purge tasks kept out of {@code CustomerApplication}
 * to avoid DevTools restart race conditions where the main class is wired
 * before dependent beans (e.g. SubscriptionService) are fully registered.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduledTasks {

    private final SubscriptionService subscriptionService;
    private final UsersRepository usersRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final PurgeService purgeService;
    private final TrialStatusRepository trialStatusRepository;
    private final EmailService emailService;

    @Value("${purge.expiry.days}")
    private int purgeExpiryDays;

    @Scheduled(cron = "0 0 0 * * ?")
    public void updateAllSubscriptionExpiry() {
        log.info("updateAllSubscriptionExpiryTriggered->{}", LocalDateTime.now());
        subscriptionService.isExpired();
        List<Subscription> subscriptionList = subscriptionRepository.findByStatus(SubscriptionEnum.ACTIVE);
        subscriptionList.forEach((data) -> {
            long activeUserCount = usersRepository.countByCompanyIdAndStatus(data.getCompanyId(), com.quantumai.customer.entity.StatusEnum.active);
            if (activeUserCount > data.getPerson()) {
                log.info("Notification Sent For Inactivate user or upgrade subscription to avoid account lock for company Id {}", data.getCompanyId());
                Notification notification = new Notification("", "ALERT!!",
                        "Alert!! Please Inactivate user or upgrade subscription to avoid account lock",
                        NotificationType.COMPANY, "", data.getCompanyId(), LocalDateTime.now(), null);
                notificationService.sendNotificationToAdmin(data.getCompanyId(), notification);
            }
        });

        log.info("Purge Service Called");
        purgeService.purgeOldData();
        expirySendNotification();
    }

    private void expirySendNotification() {
        log.info("Notification Sent For Inactivate user or upgrade subscription to avoid account lock for company Id ");
        trialStatusRepository.findByTrialEndDateBetween(LocalDateTime.now().minusDays(purgeExpiryDays), LocalDateTime.now())
                .forEach(trialStatus -> {
                    Optional<Subscription> data = subscriptionRepository.findByCompanyIdAndStatus(
                            trialStatus.getCompanyId(), SubscriptionEnum.ACTIVE);
                    if (data.isEmpty()) {
                        Long companyId = trialStatus.getCompanyId();
                        log.info("Trial Expiry Notification Sent For Company Id {}", companyId);
                        Notification notification = new Notification("", "ALERT!!",
                                "Alert!! Your Trial Has Ended\n" +
                                        "We'll keep your data for the next " + purgeExpiryDays + " days.\n" +
                                        "Upgrade now to continue accessing your account.",
                                NotificationType.COMPANY, "", companyId, LocalDateTime.now(), null);
                        notificationService.sendNotificationToAdmin(companyId, notification);
                        emailToAdmin(companyId);
                    }
                });
    }

    private void emailToAdmin(Long companyId) {
        List<Users> userList = usersRepository.findByCompanyId(companyId);
        userList = userList.stream().filter((user) -> user.getRole().getName().equals("ADMIN")).toList();
        userList.forEach((user) -> emailService.sendEmail(user.getEmail(), "AssetYug - Trial Ended",
                "The trial period for your account has ended. Please note that we will retain your data for the next "
                        + purgeExpiryDays + " days. During this time, you may choose to upgrade your plan to continue accessing your data and services without interruption.\n"
                        + "\n"
                        + "If no action is taken within 3 days, the data associated with your trial account may be permanently deleted.\n"
                        + "\n"
                        + "If you have any questions or need assistance, feel free to reach out.",
                user.getFirstName()));
    }
}
