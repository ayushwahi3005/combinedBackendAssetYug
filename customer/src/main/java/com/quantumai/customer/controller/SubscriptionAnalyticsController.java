package com.quantumai.customer.controller;

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
public class SubscriptionAnalyticsController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionAnalyticsController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/growth")
    public ResponseEntity<List<SubscriptionGrowthDTO>> getSubscriptionGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionGrowth(startDate, endDate));
    }

    @GetMapping("/analytics")
    public ResponseEntity<List<SubscriptionAnalyticsDTO>> getSubscriptionAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam SubscriptionAnalyticsDTO.TimePeriod period) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionAnalytics(startDate, endDate, period));
    }

    @GetMapping("/churn-rate")
    public ResponseEntity<Double> getChurnRate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(subscriptionService.calculateChurnRate(startDate, endDate));
    }

    @GetMapping("/revenue-trends")
    public ResponseEntity<List<RevenueTrendDTO>> getRevenueTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam SubscriptionAnalyticsDTO.TimePeriod period) {
        return ResponseEntity.ok(subscriptionService.getRevenueTrends(startDate, endDate, period));
    }

    @GetMapping("/mrr-trend")
    public ResponseEntity<List<MrrDTO>> getMrrTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(subscriptionService.getMrrTrend(startDate, endDate));
    }
    @GetMapping("/check")
    public ResponseEntity<String> check() {
        return ResponseEntity.ok("Working");
    }
}
