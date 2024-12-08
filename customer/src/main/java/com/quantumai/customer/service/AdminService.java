package com.quantumai.customer.service;

import com.quantumai.customer.dto.AdminResetPassword;
import com.quantumai.customer.dto.AuthenticationRequestDTO;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.exception.UserNotFound;
import org.springframework.mail.javamail.JavaMailSender;

public interface AdminService {

    public AuthenticationResponseDTO login(Admin admin,String deviceId) throws Exception;

    public void updatePassword(AdminResetPassword adminResetPassword) throws Exception;

    public String generateOtp(String email) throws Exception;

    public void sendOtpEmail(String to, String otp) throws Exception;

    public boolean validateOtp(String email, String otp);
    public void clearOtp(String email);

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO authenticationRequestDTO,String deviceId) throws Exception;
    public AuthenticationResponseDTO getLoginToken(String email,String deviceId) throws UserNotFound;


}
