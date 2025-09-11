package com.quantumai.customer.interceptor;

import com.quantumai.customer.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingConfig rateLimitingConfig;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                           @NonNull HttpServletResponse response, 
                           @NonNull Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String ipAddress = getClientIp(request);
        
        log.debug("Rate limit check - IP: {}, URI: {}", ipAddress, requestURI);
        
        Bucket bucket;

        try {
            if (requestURI.contains("/login") || requestURI.contains("/authenticate")) {
                log.debug("Using login rate limit for IP: {}", ipAddress);
                bucket = rateLimitingConfig.resolveLoginBucket(ipAddress);
            } else {
                bucket = rateLimitingConfig.resolveBucket(ipAddress);
            }

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                long remaining = probe.getRemainingTokens();
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
                log.debug("Request allowed - IP: {}, Remaining: {}", ipAddress, remaining);
                return true;
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                String message = String.format("Rate limit exceeded for IP: %s. Please try again in %d seconds.", 
                    ipAddress, waitForRefill);
                log.warn("Rate limit exceeded - IP: {}, URI: {}", ipAddress, requestURI);
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), message);
                return false;
            }
        } catch (Exception e) {
            log.error("Error in rate limiting for IP: {}, URI: {}", ipAddress, requestURI, e);
            // In case of error, allow the request to proceed
            return true;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(",")) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
