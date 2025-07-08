package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;

public class UserNotificationRepositoryImpl {

    @Autowired
    MongoTemplate mongoTemplate;
    public List<UserNotification> findRecentNotificationsWithDetails(String email){
        Query query=new Query();
//        query.addCriteria(Criteria.where("isRead").is(false).and("deliveredAt").gte(LocalDateTime.now().minusMonths(1)).and("userId").is(email));
        query.addCriteria(Criteria.where("userId").is(email)
                .orOperator(
                        new Criteria().andOperator(
                                Criteria.where("isRead").is(false),
                                Criteria.where("deliveredAt").gte(LocalDateTime.now().minusMonths(1))
                        ),
                        new Criteria().andOperator(
                                Criteria.where("isRead").is(true),
                                Criteria.where("deliveredAt").gte(LocalDateTime.now().minusWeeks(1))
                        )));
        List<UserNotification> notificationList=mongoTemplate.find(query, UserNotification.class);
        return notificationList;

    }
}
