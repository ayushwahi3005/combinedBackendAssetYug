package com.quantumai.customer.controller;


import com.quantumai.customer.dto.AdminResetPassword;
import com.quantumai.customer.dto.AssetsDTO;
import com.quantumai.customer.dto.AuthenticationRequestDTO;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.exception.UserException;
import com.quantumai.customer.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "**")
@Slf4j
public class AdminAPI {


    @Autowired
    private AdminService adminService;
    @PostMapping("/login")
    public void login(@RequestBody Admin admin) throws Exception {



        adminService.login(admin);

    }
    @PostMapping("/resetPassword")
    public void resetPassword(@RequestBody AdminResetPassword adminResetPassword) throws Exception {



        adminService.updatePassword(adminResetPassword);

    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody String email) throws Exception {
//        String email = request.get("email");

//        try{
//            Thread.sleep(5000);
//        }
//        catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // Restore interrupted status
//            System.out.println("Thread was interrupted.");
//        }
        adminService.generateOtp(email);

        return ResponseEntity.ok("OTP sent to email");
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<Boolean> validateOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        boolean isValid = adminService.validateOtp(email, otp);
        if (isValid) {
            adminService.clearOtp(email);
        }
        return ResponseEntity.ok(isValid);
    }

    @PostMapping(value="/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticate(@RequestBody AuthenticationRequestDTO authenticationRequestDTO) throws Exception{


        log.info("Email and Password {} {}",authenticationRequestDTO.getEmail(),authenticationRequestDTO.getPassword());
        return ResponseEntity.ok(adminService.authenticate(authenticationRequestDTO));
    }

    @GetMapping(value="/getLoginToken/{email}")
    public ResponseEntity<AuthenticationResponseDTO> getLoginToken(@PathVariable String email) throws Exception {

        return ResponseEntity.ok(adminService.getLoginToken(email));
    }
}
