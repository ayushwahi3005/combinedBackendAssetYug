package com.quantumai.customer.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class RateLimitingConfig {

    // Increased rate limits
    private static final int GENERAL_REQUESTS_PER_MINUTE = 50;
    private static final int LOGIN_ATTEMPTS_PER_MINUTE = 10;
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return buckets;
    }

    public Bucket createNewBucket() {
        log.info("Creating new rate limit bucket: {} requests per minute", GENERAL_REQUESTS_PER_MINUTE);
        return Bucket.builder()
                .addLimit(Bandwidth.classic(GENERAL_REQUESTS_PER_MINUTE, 
                         Refill.intervally(GENERAL_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))))
                .build();
    }

    public Bucket createLoginBucket() {
        log.info("Creating login rate limit bucket: {} requests per minute", LOGIN_ATTEMPTS_PER_MINUTE);
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15))))
                .build();
    }

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createNewBucket());
    }

    public Bucket resolveLoginBucket(String key) {
        return buckets.computeIfAbsent("login_" + key, k -> createLoginBucket());
    }
}
