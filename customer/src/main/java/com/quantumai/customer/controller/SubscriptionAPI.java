package com.quantumai.customer.controller;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.CompanyInformation;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.repository.CompanyInformationRepository;
import com.quantumai.customer.service.SubscriptionService;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@RestController
@RequestMapping("/subscription")
public class SubscriptionAPI {

  @Autowired private SubscriptionService subscriptionService;

  @Autowired private CompanyInformationRepository companyInformationRepository;

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

  @DeleteMapping("/deleteUpcomingSubscription/{companyId}/{email}")
  public void deleteUpcomingSubscription(
      @PathVariable Long companyId, @PathVariable String email)
      throws Exception {
    //
    Optional<CompanyInformation> companyInformation=companyInformationRepository.findById(companyId);
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());
    companyInformation.ifPresent((data)->{
        try {
            subscriptionService.deleteUpcomingSubscription(companyId, data.getCompanyName(), email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });


  }

  @GetMapping("/startUpcomingSubscription/{companyId}/{email}")
  public void startUpcomingSubscription(
      @PathVariable Long companyId, @PathVariable String email)
      throws Exception {

     System.out.println("-------startUpcoming---->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());

    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());
    Optional<CompanyInformation> companyInformation=companyInformationRepository.findById(companyId);
    companyInformation.ifPresent((data)->{
        try {
            subscriptionService.startUpcomingSubscription(companyId, data.getCompanyName(), email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

  }
}
