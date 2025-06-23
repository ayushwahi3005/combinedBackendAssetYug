package com.quantumai.customer.config;

import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    JwtService jwtService;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    CustomerRepository customerRepository;
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/assetyug-notifications")
                .setAllowedOrigins("http://localhost:4200")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
//        registry.setApplicationDestinationPrefixes("/app");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                // Only validate token during CONNECT
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    // Check if Authorization header exists and is properly formatted
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new MessagingException("Missing or invalid authorization header");
                    }

                    String jwt = authHeader.substring(7);
                    String userEmail = jwtService.extractUserEmail(jwt);

                    // Check if token contains a valid email
                    if (userEmail == null) {
                        throw new MessagingException("Invalid JWT token - no user email");
                    }

                    // Find user in database (admin or customer)
                    Optional<Customer> userDetails = customerRepository.findByEmail(userEmail);
                    if (userDetails.isEmpty()) {
                        throw new MessagingException("User not found");
                    }

                    // Validate token against user details

                    if (!jwtService.isTokenValid(jwt,userDetails.get())) {
                        throw new MessagingException("Invalid token for user");
                    }


                }
                return message;
            }

//            private UserDetails findUserDetails(String userEmail) {
//                // Check admin repository first
//                Optional<Admin> admin = adminRepository.findByEmail(userEmail);
//                if (admin.isPresent()) {
//                    return admin.get();
//                }
//
//                // Then check customer repository
//                Optional<Customer> customer = customerRepository.findByEmail(userEmail);
//                return customer.orElse(null);
//            }
        });
    }
}
