package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.quantumai.customer.dto.MrrDTO;
import com.quantumai.customer.dto.RevenueTrendDTO;
import com.quantumai.customer.dto.SubscriptionAnalyticsDTO;
import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import com.quantumai.customer.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/api/analytics/subscriptions")
@Tag(name = "SubscriptionAnalytics", description = "SubscriptionAnalytics Management API")
public class SubscriptionAnalyticsController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionAnalyticsController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(summary = "Get Subscription Growth", description = "Endpoint to get subscription growth")
    @GetMapping("/growth")
    public ResponseEntity<List<SubscriptionGrowthDTO>> getSubscriptionGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionGrowth(startDate, endDate));
    }

    @Operation(summary = "Get Subscription Analytics", description = "Endpoint to get subscription analytics")
    @GetMapping("/analytics")
    public ResponseEntity<List<SubscriptionAnalyticsDTO>> getSubscriptionAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam SubscriptionAnalyticsDTO.TimePeriod period) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionAnalytics(startDate, endDate, period));
    }

    @Operation(summary = "Get Churn Rate", description = "Endpoint to get churn rate")
    @GetMapping("/churn-rate")
    public ResponseEntity<Double> getChurnRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(subscriptionService.calculateChurnRate(startDate, endDate));
    }

    @Operation(summary = "Get Revenue Trends", description = "Endpoint to get revenue trends")
    @GetMapping("/revenue-trends")
    public ResponseEntity<List<RevenueTrendDTO>> getRevenueTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam SubscriptionAnalyticsDTO.TimePeriod period) {
        return ResponseEntity.ok(subscriptionService.getRevenueTrends(startDate, endDate, period));
    }

    @Operation(summary = "Get Mrr Trend", description = "Endpoint to get mrr trend")
    @GetMapping("/mrr-trend")
    public ResponseEntity<List<MrrDTO>> getMrrTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(subscriptionService.getMrrTrend(startDate, endDate));
    }
    @Operation(summary = "Check", description = "Endpoint to check")
    @GetMapping("/check")
    public ResponseEntity<String> check() {
        return ResponseEntity.ok("Working");
    }
}
