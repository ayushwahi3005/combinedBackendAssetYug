package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserNotification;
import com.quantumai.customer.dto.UserNotificationDTO;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UserNotificationRepository  extends MongoRepository<UserNotification, String> , CompanyScopedRepository{

    List<UserNotification> findByUserIdAndIsReadFalseAndDeliveredAtAfter(String userId, LocalDateTime cutoff);
    List<UserNotification> findByUserIdAndNotificationId(String userId, String notificationId);
    void deleteByDeliveredAtBefore(LocalDateTime cutoff);

    @Aggregation(pipeline = {
            "{ $match: { " +
                    " userId: ?0, " +
                    " $or: [ " +
                    "   { isRead: true, readAt: { $gte: ?1 } }, " +
                    "   { isRead: false, deliveredAt: { $gte: ?2 } } " +
                    " ] " +
                    "} }",
            "{ $lookup: { " +
                    "   from: 'notification', " +
                    "   localField: 'notificationId', " +
                    "   foreignField: 'id', " +
                    "   as: 'notification' " +
                    "} }",
            "{ $unwind: '$notification' }",
            "{ $sort: { deliveredAt: -1 } }"
    })
    List<UserNotificationDTO> findRecentNotificationsWithDetails(
            String userId,
            LocalDateTime sevenDaysAgo,
            LocalDateTime thirtyDaysAgo
    );


    public void deleteByCompanyId(Long companyId);
}
