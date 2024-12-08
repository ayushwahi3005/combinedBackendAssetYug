package com.quantumai.customer.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.service.PaymentService;
import com.quantumai.customer.service.SubscriptionService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@CrossOrigin(origins = "**")
public class PaymentAPI {

    @Autowired
    private SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    public PaymentAPI(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody JsonNode obj) {
        System.out.println("===>Create Intent");
        try {
            String clientSecret = paymentService.createPaymentIntent(obj.get("amount").asLong(), obj.get("currency").asText());
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public void addPayment(@RequestBody Payment payment){
        		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace(); // Handle the exception if needed
		}
        subscriptionService.addPayment(payment);
    }
    @PutMapping("/update")
    public void updatePayment(@RequestBody Payment Payment){

        subscriptionService.updatePayment(Payment);
    }
    @GetMapping("/checkCred")
    public ResponseEntity<Boolean> checkCred(){

        return ResponseEntity.ok(true);
    }
}
