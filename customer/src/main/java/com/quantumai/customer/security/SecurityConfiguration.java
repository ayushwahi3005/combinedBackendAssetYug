package com.quantumai.customer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final AuthenticationProvider authenticationProvider;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf()
				.disable()
				.authorizeHttpRequests()
				.requestMatchers("/customer/addCustomer","/customer/getLoginToken/**","/customer/addCompanyInformation","/customer/getCompanyId/**","/customer/addUser","/customer/accountInfo/**","/customer/working/**","/users/invite/getUser/**","/customer/authenticate/**")
//			.requestMatchers("**")
				.permitAll()
				.anyRequest()
				.authenticated()
				.and()
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

//    @Bean
//    WebMvcConfigurer corsConfigurer() {
//	        return new WebMvcConfigurer() {
//	            @Override
//	            public void addCorsMappings(CorsRegistry registry) {
//	            	registry.addMapping("/**")
//	                .allowedOrigins("http://localhost:4200")
////	                .allowedOrigins("**")
//	                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//	                .allowedHeaders("*", "Authorization")
//	                .exposedHeaders("Authorization")
//	                .allowCredentials(true)
//	                .maxAge(3600);
//	            }
//	        };
//	    }
}