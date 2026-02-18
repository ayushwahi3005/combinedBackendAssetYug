package com.quantumai.customer.service;

import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserActivationServiceImpl implements UserActivationService {

    private final UsersRepository usersRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public UserActivationServiceImpl(UsersRepository usersRepository,
                                   SubscriptionRepository subscriptionRepository) {
        this.usersRepository = usersRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional
    public int updateActiveUsersBySubscription(Long companyId, int activeLimit) {
        // Get all active users for the company, ordered by last login (most recent first)
        List<Users> activeUsers = usersRepository.findByCompanyIdAndStatus(companyId, StatusEnum.active);

        // If we're under or at the limit, nothing to do
        if (activeUsers.size() <= activeLimit) {
            return 0;
        }

        // Sort users by last login (most recent first)
        // Note: You may need to add lastLogin field to Customer entity if not exists
        activeUsers.sort(Comparator.comparing(
            c -> c.getLastLogin() != null ? c.getLastLogin() : LocalDateTime.MIN,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // Deactivate users beyond the limit (least recently used first)
        int deactivated = 0;
        for (int i = activeLimit; i < activeUsers.size(); i++) {
            Users user = activeUsers.get(i);
            user.setStatus(UserStatusEnum.inActive);
            usersRepository.save(user);
            deactivated++;
            log.info("Deactivated user {} (ID: {}) due to subscription limit",
                    user.getEmail(), user.getId());
        }

        return deactivated;
    }

    @Override
    public boolean canActivateNewUser(Long companyId) {
        // Get current active subscription
        Optional<Subscription> activeSubscription = subscriptionRepository
                .findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);

        if (activeSubscription.isEmpty()) {
            log.warn("No active subscription found for company {}", companyId);
            return false;
        }

        int allowedUsers = activeSubscription.get().getPerson();
        log.info("AllowedUser : {}",allowedUsers);
        long activeUserCount = usersRepository.countByCompanyIdAndStatus(companyId, StatusEnum.active);

        return activeUserCount < allowedUsers;
    }

    @Override
    @Transactional
    public boolean deactivateUser(String userId) {
        return usersRepository.findById(userId)
                .map(user -> {
                    if (user.getStatus().equals(StatusEnum.active)) {
                        user.setStatus(UserStatusEnum.inActive);
                        usersRepository.save(user);
                        log.info("Deactivated user {} (ID: {})", user.getEmail(), user.getId());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean activateUser(String userId) {
        return usersRepository.findById(userId)
                .map(user -> {
                    if (!user.getStatus().equals(StatusEnum.active)) {
                        // Check if we can activate this user based on subscription
                        if (canActivateNewUser(user.getCompanyId())) {
                            user.setStatus(UserStatusEnum.active);
                            usersRepository.save(user);
                            log.info("Activated user {} (ID: {})", user.getEmail(), user.getId());
                            return true;
                        } else {
                            log.warn("Cannot activate user {} - subscription limit reached", user.getEmail());
                            return false;
                        }
                    }
                    return true; // Already active
                })
                .orElse(false);
    }
}
