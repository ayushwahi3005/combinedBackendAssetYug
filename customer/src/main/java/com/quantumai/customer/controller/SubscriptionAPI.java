package com.quantumai.customer.controller;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.service.SubscriptionService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "**")
@RestController
@RequestMapping("/subscription")
public class SubscriptionAPI {

  @Autowired private SubscriptionService subscriptionService;

  @PostMapping("/add")
  public void addSubscription(@RequestBody SubscriptionDTO subscriptionDTO) {
    //        System.out.println("----------->"+subscriptionDTO);
    subscriptionService.addSubscription(subscriptionDTO);
  }

  @PostMapping("/update")
  public void updateSubscription(@RequestBody SubscriptionDTO subscriptionDTO) {
    //        System.out.println("----------->"+subscriptionDTO);
    subscriptionService.updateSubscription(subscriptionDTO);
  }

  @GetMapping("/currentSubscription/{companyId}")
  public Subscription currentSubscription(@PathVariable Long companyId) {
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());
    System.out.println("----------->" + subscriptionService.getCurrentSubscription(companyId));
    return subscriptionService.getCurrentSubscription(companyId);
  }

  @GetMapping("/getAllSubscription/{companyId}")
  public List<Subscription> getAllSubscription(@PathVariable Long companyId) {
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());

    return subscriptionService.getAllSubscription(companyId);
  }

  @DeleteMapping("/deleteUpcomingSubscription/{companyId}/{companyName}/{email}")
  public void deleteUpcomingSubscription(
      @PathVariable Long companyId, @PathVariable String companyName, @PathVariable String email)
      throws Exception {
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());

    subscriptionService.deleteUpcomingSubscription(companyId, companyName, email);
  }

  @GetMapping("/startUpcomingSubscription/{companyId}/{companyName}/{email}")
  public void startUpcomingSubscription(
      @PathVariable Long companyId, @PathVariable String companyName, @PathVariable String email)
      throws Exception {
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());
    subscriptionService.startUpcomingSubscription(companyId, companyName, email);
  }
}
