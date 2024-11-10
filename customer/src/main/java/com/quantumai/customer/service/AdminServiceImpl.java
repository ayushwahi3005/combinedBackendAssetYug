package com.quantumai.customer.service;

import com.quantumai.customer.dto.AdminResetPassword;
import com.quantumai.customer.dto.AuthenticationRequestDTO;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.exception.OTPException;
import com.quantumai.customer.exception.UserNotFound;
import com.quantumai.customer.exception.WrongAdminEmailException;
import com.quantumai.customer.repository.AdminRepository;
import com.quantumai.customer.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
//    @Qualifier("adminAuthenticationProvider")
    private AuthenticationManager authenticationManager;



    @Autowired
    JwtService jwtService;

    private final Map<String, String> otpStorage = new HashMap<>();

    @Override
    public Admin login(Admin admin) throws Exception {
        List<Admin> adminList=adminRepository.findAll();
        if(!adminList.isEmpty()) {
            Admin admin1 = adminList.get(0);
            if(passwordEncoder.matches(admin.getPassword(),admin1.getPassword())&&admin.getEmail().equals(admin1.getEmail())){
                return admin;
            }
            else{
                return null;
            }
        }
        else{
            log.error("No Admin found");
            throw new Exception("No Admin");
        }

    }

    @Override
    public void updatePassword(AdminResetPassword adminResetPassword) throws Exception {
        List<Admin> adminList=adminRepository.findAll();

        if(!adminList.isEmpty()) {
            Admin admin1 = adminList.get(0);
            if(adminResetPassword.getEmail().equals(admin1.getEmail())){
                admin1.setPassword(passwordEncoder.encode(adminResetPassword.getPassword())  );
                adminRepository.save(admin1);
            }
            else{
                throw new OTPException("Wrong OTP or Email");
            }

        }
        else{
            log.error("No Admin found");
            throw new Exception("No Admin");
        }




    }

    @Override
    public String generateOtp(String email) throws Exception {
        String otp = String.valueOf((int) (Math.random() * 9000) + 1000); // Generate 4-digit OTP
        otpStorage.put(email, otp);
        sendOtpEmail(email, otp);
        return otp;
    }

    @Override
    public void sendOtpEmail(String to, String otp) throws Exception {
        List<Admin> adminList=adminRepository.findAll();
        if(!adminList.isEmpty()) {
            Admin admin1 = adminList.get(0);
            if(to.equals(admin1.getEmail())){
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject("OTP for Password Reset");
                message.setText("Your OTP for password reset is: " + otp);
                mailSender.send(message);
            }
            else{
                throw new WrongAdminEmailException("Wrong Admin Email");
            }
        }
        else{
            log.error("No Admin found");
            throw new Exception("No Admin");
        }


    }
    @Override
    public boolean validateOtp(String email, String otp) {
        return otp.equals(otpStorage.get(email));
    }
    @Override
    public void clearOtp(String email) {
        otpStorage.remove(email);
    }

    @Override
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO authenticationRequestDTO) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequestDTO.getEmail(),authenticationRequestDTO.getPassword())
        );
        var user=adminRepository.findByEmail(authenticationRequestDTO.getEmail()).orElseThrow(()->new Exception("Not Present"));
        var jwtToken=jwtService.generateToken(user);
        return AuthenticationResponseDTO.builder().token(jwtToken).build();
    }

    @Override
    public AuthenticationResponseDTO getLoginToken(String email) throws UserNotFound {
        Optional<Admin> admin=adminRepository.findByEmail(email);
        System.out.println("////"+admin);
        if(admin.isEmpty()) {
            throw new UserNotFound("User Not Associated to any company");
        }
//		System.out.print(customer);
        var jwtToken=jwtService.generateToken(admin.get());


        return AuthenticationResponseDTO.builder().token(jwtToken).role(admin.get().getRole()).build();
    }
}
