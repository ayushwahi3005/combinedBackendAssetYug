package com.quantumai.customer.controller;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "**")
@RestController
@RequestMapping("/subscription")
public class SubscriptionAPI {

    @Autowired
    private SubscriptionService subscriptionService;


    @PostMapping("/add")
    public void addSubscription(@RequestBody SubscriptionDTO subscriptionDTO){
        System.out.println("----------->"+subscriptionDTO);
        subscriptionService.addSubscription(subscriptionDTO);
    }
    @PostMapping("/update")
    public void updateSubscription(@RequestBody SubscriptionDTO subscriptionDTO){
        System.out.println("----------->"+subscriptionDTO);
        subscriptionService.updateSubscription(subscriptionDTO);
    }
}
