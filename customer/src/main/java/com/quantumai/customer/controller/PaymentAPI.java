package com.quantumai.customer.controller;


import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@CrossOrigin(origins = "**")
public class PaymentAPI {

    @Autowired
    private SubscriptionService subscriptionService;


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
}
