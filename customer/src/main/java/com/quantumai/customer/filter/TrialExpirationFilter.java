package com.quantumai.customer.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.security.JwtService;
import com.quantumai.customer.service.TrialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TrialExpirationFilter extends OncePerRequestFilter {

    @Autowired
    private TrialService trialService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    CustomerRepository customerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Endpoints that should be excluded from trial check
    private final List<String> excludedPaths = Arrays.asList(
            "/customer/addCustomer",
            "/customer/authenticate",
            "/customer/getLoginToken",
            "/customer/checkUserName",
            "/customer/sentResetOTP",
            "/customer/updatePassword",
            "/payment",
            "/customer/working",
            "/notification");

    private boolean shouldSkipTrialCheck(String requestPath, String method) {
        // Skip for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && requestPath.startsWith("/assets/advanceFilter")) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && requestPath.startsWith("/companycustomer/advanceFilter/")) {
            return true;
        }
        // Skip for excluded paths
        for (String excludedPath : excludedPaths) {
            if (requestPath.startsWith(excludedPath)) {
                return true;
            }
        }

        // Skip for static resources
        if (requestPath.contains("/static/") ||
                requestPath.endsWith(".js") ||
                requestPath.endsWith(".css") ||
                requestPath.endsWith(".html") ||
                requestPath.endsWith(".ico")) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
            throws jakarta.servlet.ServletException, IOException {
        // TODO Auto-generated method stub
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // Skip trial check for excluded paths
        if (shouldSkipTrialCheck(requestPath, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                String userEmail = jwtService.extractUserEmail(jwt);

                if (userEmail != null) {
                    java.util.Optional<Customer> customerOptional = customerRepository.findByEmail(userEmail);
                    if (customerOptional.isPresent()) {
                        java.util.Optional<Subscription> subscriptionOptional = subscriptionRepository
                                .findByCompanyIdAndStatus(customerOptional.get().getCompanyId(),
                                        SubscriptionEnum.ACTIVE);
                        // Check if trial has expired
                        if (subscriptionOptional.isEmpty()&&trialService.isTrialExpired(userEmail)) {
                            // Return JSON response indicating trial expiration
                            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED); // 402
                            response.setContentType("application/json");

                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("error", "TRIAL_EXPIRED");
                            errorResponse.put("message",
                                    "Your free trial/plan has expired. Please upgrade to continue using our services.");
                            errorResponse.put("redirectTo", "/payment");
                            errorResponse.put("trialExpired", true);

                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                            return;
                        }
                    } else {
                        throw new Exception("No such username");
                    }

                }
            } catch (Exception e) {
                // If there's an error extracting user info, continue with the request
                // The authentication will be handled by other security filters
            }
        }

        filterChain.doFilter(request, response);
    }
}
