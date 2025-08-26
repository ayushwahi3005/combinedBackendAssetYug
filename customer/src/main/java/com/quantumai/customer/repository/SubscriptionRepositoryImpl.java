package com.quantumai.customer.repository;

import com.quantumai.customer.dto.MrrDTO;
import com.quantumai.customer.dto.RevenueTrendDTO;
import com.quantumai.customer.dto.SubscriptionAnalyticsDTO;
import com.quantumai.customer.dto.SubscriptionAnalyticsDTO.TimePeriod;
import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;

@Repository("subscriptionRepositoryImpl")
public class SubscriptionRepositoryImpl implements SubscriptionRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<SubscriptionGrowthDTO> getSubscriptionGrowth(LocalDate start, LocalDate end) {
        // Validate input parameters
        Assert.notNull(start, "Start date must not be null");
        Assert.notNull(end, "End date must not be null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        // Build aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("subscriptionDate").gte(start).lte(end)),
                Aggregation.group("subscriptionDate")
                        .sum(ConditionalOperators.when(Criteria.where("status").is(SubscriptionEnum.ACTIVE)).then(1).otherwise(0)).as("newSubscriptions")
                        .sum(ConditionalOperators.when(Criteria.where("status").is(SubscriptionEnum.EXPIRED)).then(1).otherwise(0)).as("cancelledSubscriptions"),
                Aggregation.project("newSubscriptions", "cancelledSubscriptions")
                        .and("_id").as("date")
                        .andExpression("newSubscriptions - cancelledSubscriptions").as("netGrowth")
        );
        AggregationResults<SubscriptionGrowthDTO> results = mongoTemplate.aggregate(aggregation, Subscription.class, SubscriptionGrowthDTO.class);
        return results.getMappedResults();
    }

    @Override
    public List<SubscriptionAnalyticsDTO> getSubscriptionAnalytics(LocalDate start, LocalDate end, TimePeriod period) {
        Assert.notNull(start, "Start date must not be null");
        Assert.notNull(end, "End date must not be null");
        Assert.notNull(period, "Time period must not be null");

        MatchOperation matchStage = Aggregation.match(Criteria.where("subscriptionDate").gte(start).lte(end));

        // First projection to keep necessary fields and create a grouping key
        ProjectionOperation projectToCreateGroupKey = Aggregation.project("status", "amount")
                .and(getPeriodExpression(period)).as("periodKey");

        GroupOperation groupStage = Aggregation.group("periodKey")
                .sum(ConditionalOperators.when(Criteria.where("status").is(SubscriptionEnum.ACTIVE)).then(1).otherwise(0)).as("newSubscriptions")
                .sum(ConditionalOperators.when(Criteria.where("status").is(SubscriptionEnum.EXPIRED)).then(1).otherwise(0)).as("cancelledSubscriptions")
                .sum("amount").as("revenue");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, projectToCreateGroupKey, groupStage);

        AggregationResults<SubscriptionAnalyticsDTO> results = mongoTemplate.aggregate(aggregation, Subscription.class, SubscriptionAnalyticsDTO.class);
        return results.getMappedResults();
    }

    @Override
    public double calculateChurnRate(LocalDate start, LocalDate end) {
        Assert.notNull(start, "Start date must not be null");
        Assert.notNull(end, "End date must not be null");

        // Customers active at the start of the period
        long activeAtStart = mongoTemplate.count(
            new org.springframework.data.mongodb.core.query.Query(
                Criteria.where("subscriptionDate").lt(start)
                        .and("status").is(SubscriptionEnum.ACTIVE)
            ), 
            Subscription.class
        );

        // Customers who churned during the period
        long churnedCount = mongoTemplate.count(
            new org.springframework.data.mongodb.core.query.Query(
                Criteria.where("expiryDate").gte(start).lte(end)
                        .and("status").is(SubscriptionEnum.EXPIRED)
            ), 
            Subscription.class
        );
        
        if (activeAtStart == 0) {
            return 0.0;
        }

        return ((double) churnedCount / activeAtStart) * 100.0;
    }

    @Override
    public List<RevenueTrendDTO> getRevenueTrends(LocalDate start, LocalDate end, TimePeriod period) {
        Assert.notNull(start, "Start date must not be null");
        Assert.notNull(end, "End date must not be null");
        Assert.notNull(period, "Time period must not be null");

        MatchOperation matchStage = Aggregation.match(Criteria.where("subscriptionDate").gte(start).lte(end));

        ProjectionOperation projectToCreateGroupKey = Aggregation.project("amount")
                .and(getPeriodExpression(period)).as("periodKey");

        GroupOperation groupStage = Aggregation.group("periodKey")
            .sum("amount").as("totalRevenue")
            .count().as("totalTransactions");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, projectToCreateGroupKey, groupStage);
        
        return mongoTemplate.aggregate(aggregation, Subscription.class, RevenueTrendDTO.class).getMappedResults();
    }

    @Override
    public List<MrrDTO> getMrrTrend(LocalDate start, LocalDate end) {
        Assert.notNull(start, "Start date must not be null");
        Assert.notNull(end, "End date must not be null");

        MatchOperation matchStage = Aggregation.match(
            Criteria.where("status").is(SubscriptionEnum.ACTIVE)
                    .and("subscriptionDate").lte(end)
        );

        ProjectionOperation projectToCreateGroupKey = Aggregation.project("amount")
                .and(DateOperators.dateOf("subscriptionDate").toString("%Y-%m")).as("periodKey");

        GroupOperation groupStage = Aggregation.group("periodKey")
            .sum("amount").as("mrrAmount")
            .count().as("activeSubscriptions");
            
        ProjectionOperation projectionStage = Aggregation.project("mrrAmount", "activeSubscriptions")
            .and("_id").as("date")
            .andExpression("mrrAmount / activeSubscriptions").as("averageRevenuePerUser");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, projectToCreateGroupKey, groupStage, projectionStage);

        return mongoTemplate.aggregate(aggregation, Subscription.class, MrrDTO.class).getMappedResults();
    }

    private AggregationExpression getPeriodExpression(TimePeriod period) {
        switch (period) {
            case DAILY:
                return DateOperators.dateOf("subscriptionDate").toString("%Y-%m-%d");
            case WEEKLY:
                return DateOperators.dateOf("subscriptionDate").toString("%Y-%U");
            case MONTHLY:
                return DateOperators.dateOf("subscriptionDate").toString("%Y-%m");
            case QUARTERLY:
                return ArithmeticOperators.Ceil.ceilValueOf(
                    ArithmeticOperators.Divide.valueOf(
                        DateOperators.dateOf("subscriptionDate").isoWeek()
                    ).divideBy(13)
                );
            case YEARLY:
                return DateOperators.dateOf("subscriptionDate").toString("%Y");
            default:
                throw new IllegalArgumentException("Unsupported time period: " + period);
        }
    }
}
