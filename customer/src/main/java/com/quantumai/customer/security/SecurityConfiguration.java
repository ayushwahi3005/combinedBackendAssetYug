package com.quantumai.customer.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final JwtAdminAuthenticationFilter jwtAdminAuthFilter;
	@Autowired
	@Qualifier("customerAuthProvider")
	private AuthenticationProvider customerAuthenticationProvider;

	@Autowired
	@Qualifier("adminAuthProvider")
	private AuthenticationProvider adminAuthenticationProvider;


	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http .cors() // Enable CORS
				.and()

				.csrf()
				.disable()
				.authorizeHttpRequests()

				// Public endpoints for customer paths
				.requestMatchers(
						"/customer/addCustomer",
						"/customer/getLoginToken/**",
						"/customer/addCompanyInformation",
						"/customer/getCompanyId/**",
						"/customer/addUser",
						"/customer/accountInfo/**",
						"/customer/working/**",
						"/users/invite/getUser/**",
						"/customer/authenticate/**",
						"/customer/addLoggedIn/**",
						"/customer/isSameBrowserAndDevice/**",
						"/customer/removeSession/*"

				).permitAll()

				// Public endpoints for admin paths
				.requestMatchers(
						"/admin/authenticate/**",
						"/admin/login/**"
				).permitAll()

				// Protected paths for customers
				.requestMatchers(
						"/customer/**",
						"/assets/**",
						"/users/**",
						"/companycustomer/**",
						"/payment/**",
						"/subscription/**"
				).authenticated()

				// Protected paths for admins
				.requestMatchers("/admin/**").authenticated()

				.anyRequest().authenticated()  // Default rule to require authentication for any other request
				.and()

				// Stateless session management
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.maximumSessions(1) // Allow only one active session per user
				.sessionRegistry(sessionRegistry()) // Enable session registry
				.and()
				.and()

				// Configure different authentication providers and filters based on path
				.authenticationProvider(customerAuthenticationProvider)
				.authenticationProvider(adminAuthenticationProvider)

				// Add JWT filters based on the role paths
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtAdminAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();


	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

//	@Bean
//	public SecurityFilterChain securityFilterChain2(HttpSecurity http) throws Exception {
//		http
//				.csrf()
//				.disable()
//				.authorizeHttpRequests()
//				.requestMatchers("/admin/authenticate/**","/admin/login/**")
////			.requestMatchers("**")
//				.permitAll()
//				.requestMatchers("/admin/**")
//				.authenticated()
//				.and()
//				.sessionManagement()
//				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//				.and()
//				.authenticationProvider(adminAuthenticationProvider)
//				.addFilterBefore(jwtAdminAuthFilter, UsernamePasswordAuthenticationFilter.class);
//
//		return http.build();
//
//
//	}
//@Bean
//public CorsConfigurationSource corsConfigurationSource() {
//	CorsConfiguration config = new CorsConfiguration();
//	config.setAllowedOrigins(Arrays.asList("http://localhost:4200")); // Frontend URL
//	config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//	config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Device-ID", "device-id","x-device-id")); // Custom headers
//	config.setExposedHeaders(Arrays.asList("Authorization", "Device-ID", "device-id","x-device-id")); // Expose these headers
//	config.setAllowCredentials(true); // Allow cookies or authentication headers
//
//	UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//	source.registerCorsConfiguration("/**", config);
//	return source;
//}


}