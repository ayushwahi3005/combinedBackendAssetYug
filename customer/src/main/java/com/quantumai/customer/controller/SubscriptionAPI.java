package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.CompanyInformationRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Tag(name = "Subscription", description = "Subscription Management API")
public class SubscriptionAPI {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAPI.class);
    @Autowired private SubscriptionService subscriptionService;

  @Autowired private CompanyInformationRepository companyInformationRepository;

  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired private UsersRepository usersRepository;

  @Operation(summary = "Add Subscription", description = "Endpoint to add subscription")
  @PostMapping("/add")
  public void addSubscription(@RequestBody SubscriptionDTO subscriptionDTO) {
    //        System.out.println("----------->"+subscriptionDTO);
    subscriptionService.addSubscription(subscriptionDTO);
  }

  @Operation(summary = "Update Subscription", description = "Endpoint to update subscription")
  @PostMapping("/update")
  public void updateSubscription(@RequestBody SubscriptionDTO subscriptionDTO) {
    //        System.out.println("----------->"+subscriptionDTO);
    subscriptionService.updateSubscription(subscriptionDTO);
  }

  @Operation(summary = "Current Subscription", description = "Endpoint to current subscription")
  @GetMapping("/currentSubscription/{companyId}")
  public Subscription currentSubscription(@PathVariable Long companyId) {
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());
    System.out.println("----------->" + subscriptionService.getCurrentSubscription(companyId));
    return subscriptionService.getCurrentSubscription(companyId);
  }

  @Operation(summary = "Get All Subscription", description = "Endpoint to get all subscription")
  @GetMapping("/getAllSubscription/{companyId}")
  public List<Subscription> getAllSubscription(@PathVariable Long companyId) {
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getSubscriptionDate());
    //
    // System.out.println("----------->"+subscriptionService.getCurrentSubscription(companyId).getExpiryDate());

    return subscriptionService.getAllSubscription(companyId);
  }

  @Operation(summary = "Delete Upcoming Subscription", description = "Endpoint to delete upcoming subscription")
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

  @Operation(summary = "Start Upcoming Subscription", description = "Endpoint to start upcoming subscription")
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
    @Operation(summary = "Is Subscription Valid", description = "Endpoint to is subscription valid")
    @GetMapping("/subscription-valid/{companyId}")
    public boolean isSubscriptionValid(
            @PathVariable Long companyId)
            throws Exception {
      log.info("Subscription-valid is calling");
        Optional<Subscription> subscription=subscriptionRepository.findByCompanyIdAndStatus(companyId,SubscriptionEnum.ACTIVE);
        long activeUserCount = usersRepository.countByCompanyIdAndStatus(companyId, StatusEnum.active);
        return subscription.map(value -> value.getPerson() >= activeUserCount).orElse(true);

    }
}
