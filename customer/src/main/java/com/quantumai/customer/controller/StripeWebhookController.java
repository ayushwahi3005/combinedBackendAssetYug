package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.CustomerStripeDetailsRepository;
import com.quantumai.customer.repository.PaymentRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.service.UserActivationService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/stripe")
@Slf4j
@Tag(name = "StripeWebhook", description = "StripeWebhook Management API")
public class StripeWebhookController {

    // Must not be static; Spring does not inject into static fields.
    @Value("${stripe.webhook.endpoint}")
    private String endpointSecret;

    @Autowired
    private CustomerStripeDetailsRepository customerStripeDetailsRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private UserActivationService userActivationService;

    @Transactional
    @Operation(summary = "Handle Stripe Webhook", description = "Endpoint to handle stripe webhook")
    @PostMapping("/webhook")
    @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        log.info("Webhook Working....................."+sigHeader);
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            log.error("Webhook Exception {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        log.info("EVENT------->"+event.getType());

        // Handle invoice.payment_succeeded (used for both subscription creation and renewal)
        if ("invoice.payment_succeeded".equals(event.getType())) {
            log.info("------------------------>Inside Payment Succeeded. New Subscription");
            log.info("------------------------>Object {}",event.getObject());

            Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            log.info("Invoice data : {}",invoice);
            if(invoice==null){
                String dataObjectJson = event.getData().getObject().toJson();
                invoice = Invoice.GSON.fromJson(dataObjectJson, Invoice.class);
                log.info("Invoice data : {}",invoice);

            }

                String billingReason = invoice.getBillingReason();
                String customerId = invoice.getCustomer();
                String subscriptionId = invoice.getSubscription();
                String paymentIntent=invoice.getPaymentIntent();
                log.info("Billing Reason------->"+billingReason);
                if ("subscription_create".equals(billingReason)) {
                    // FIRST recurring payment for a new subscription
                    // TODO: Update your database, activate subscription, notify user, etc.
                    log.info("Inside Subscription Create CustomerId->: {}",customerId);
//                    Optional<CustomerStripeDetails> optionalStripeDetail=customerStripeDetailsRepository.findByCustomerId(customerId);
//                    log.info("optionalStripeDetail->: {}",optionalStripeDetail.isPresent());

                        log.info("Inside optionalStripeDetail Present");
                        List<Subscription> subscriptionList=subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                        Optional <Payment> optionalPayment=paymentRepository.findByPaymentIntentId(paymentIntent);
                        Optional<Subscription> optionalSubscriptionActive=subscriptionList.stream().filter((data)->data.getStatus().equals(SubscriptionEnum.PENDING)).findFirst();
                        optionalSubscriptionActive.ifPresent((data)->log.info("-->optionalSubscriptionActive data: {}",data.toString()));

                        if(optionalSubscriptionActive.isPresent()){
                            Subscription subscription=optionalSubscriptionActive.get();
                            Optional<Subscription> currActiveSubscription=subscriptionRepository.findByCompanyIdAndStatus(subscription.getCompanyId(),SubscriptionEnum.ACTIVE);
                            if(currActiveSubscription.isEmpty()) {
                                subscription.setStatus(SubscriptionEnum.ACTIVE);
                                subscriptionRepository.save(subscription);
                                log.info("Webhook Subscription Created And Subscription for Company {} is Changed to Active",subscription.getCompanyId());
//                                int affectedUser=userActivationService.updateActiveUsersBySubscription(subscription.getCompanyId(),subscription.getPerson());
//                                log.info("{} Users Status activated",affectedUser);
                            }


                        }
                        if(optionalPayment.isPresent()){
                            Payment myPayment=optionalPayment.get();
                            myPayment.setPaymentStatus(PaymentStatus.PAID);

                            paymentRepository.save(myPayment);
                            log.info("Webhook Subscription Created And Payment for Company {} is Changed to Paid",myPayment.getCompanyId());
                            optionalSubscriptionActive.ifPresent((data)->{
//                                int affectedUser=userActivationService.updateActiveUsersBySubscription(data.getCompanyId(),data.getPerson());
//                                log.info("{} Users Status activated",affectedUser);
                            });



                        }


                } else if ("subscription_cycle".equals(billingReason)) {
                    // RENEWAL payment for existing subscription
                    log.info("------->Subscription Cycle New Cycle update");
                    // TODO: Update your database, extend subscription, notify user, etc.
                        /// ////////////
                        //Since it is subscription cycle no explicit payment is being added
                        /// ////////////

//                        if(optionalSubscription.isPresent()){
//                            Subscription subscription=optionalSubscription.get();
//                            subscription.setStatus(SubscriptionEnum.ACTIVE);
//                            log.info("Webhook Subscription Billing Updated And Subscription for Company {} is Changed to Active",subscription.getCompanyId());
//                            subscriptionRepository.save(subscription);
//                        }
                        List<Subscription> subscriptionlist=subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                        Optional <Payment> optionalPayment=paymentRepository.findByPaymentIntentId(paymentIntent);
                        Optional<Subscription> activeSubscription=subscriptionlist.stream().filter((data)->data.getStatus().equals(SubscriptionEnum.ACTIVE)).findFirst();
                        Optional<Subscription> upcomingSubscription=subscriptionlist.stream().filter((data)->data.getStatus().equals(SubscriptionEnum.UPCOMING)).findFirst();

                    if(upcomingSubscription.isPresent()){
                            log.info("Subscription Found in Cycle Update");
                            Subscription upcomSubscription=upcomingSubscription.get();
                        upcomSubscription.setStatus(SubscriptionEnum.ACTIVE);

                            subscriptionRepository.save(upcomSubscription);


                        Subscription currSubscription=activeSubscription.get();
                        currSubscription.setStatus(SubscriptionEnum.EXPIRED);

                        subscriptionRepository.save(currSubscription);


                            log.info("Webhook Subscription With Upcoming Billing Changed And Subscription for Company {} is Changed to Active",currSubscription.getCompanyId());
//                        int affectedUser=userActivationService.updateActiveUsersBySubscription(upcomSubscription.getCompanyId(),upcomSubscription.getPerson());
//                        log.info("{} Users Status activated",affectedUser);
                        }
                    else{
                        Subscription currSubscription=activeSubscription.get();
                        currSubscription.setStatus(SubscriptionEnum.EXPIRED);
                        subscriptionRepository.save(currSubscription);
                        Subscription subscription;
                        String planName=currSubscription.getSubscriptionName();
                        if(currSubscription.getSubscriptionName().trim().isEmpty()||currSubscription.getSubscriptionName().trim().isBlank()){
                            planName="Growth";
                        }
                        if(currSubscription.getSubscriptionPlan().equals(SubscriptionPlan.MONTHLY)) {
                            subscription = new Subscription(null, currSubscription.getCompanyId(), SubscriptionEnum.ACTIVE, currSubscription.getPlan(), currSubscription.getPerson(), LocalDate.now(), LocalDate.now().plusMonths(1),currSubscription.getSubscriptionPlan(),currSubscription.getAmount(),currSubscription.getStripeSubscriptionId(),currSubscription.getStripeCustomerId(),planName);
                        }
                        else{
                            subscription = new Subscription(null, currSubscription.getCompanyId(), SubscriptionEnum.ACTIVE, currSubscription.getPlan(), currSubscription.getPerson(), LocalDate.now(), LocalDate.now().plusYears(1),currSubscription.getSubscriptionPlan(),currSubscription.getAmount(),currSubscription.getStripeSubscriptionId(),currSubscription.getStripeCustomerId(),planName);

                        }
                        log.info("New Subscription Details : {}",subscription);
                        subscriptionRepository.save(subscription);
                        log.info("Webhook Subscription With Upcoming Billing Same And Subscription for Company {} is Changed to Active",currSubscription.getCompanyId());
//                        int affectedUser=userActivationService.updateActiveUsersBySubscription(subscription.getCompanyId(),subscription.getPerson());
//                        log.info("{} Users Status activated",affectedUser);


                    }
                        if(optionalPayment.isPresent()){
                            Payment myPayment=optionalPayment.get();
                            myPayment.setPaymentStatus(PaymentStatus.PAID);

                            paymentRepository.save(myPayment);
                            log.info("Webhook Subscription Created And Payment for Company {} is Changed to Paid",myPayment.getCompanyId());

                        }

                } else {
                    // Handle other invoice payment success logic (optional)
                }

        }
        else if("payment_intent.succeeded".equals(event.getType())){
            log.info("------------------------>Object11 {}",event.getData());
            log.info("------------------------>Object22 {}",event.getData().getObject().toJson());
            JsonObject obj = JsonParser.parseString(event.getData().getObject().toJson()).getAsJsonObject();
            String paymentIntentId = obj.get("id").getAsString();
            log.info("------------------------>pyamentId {}",paymentIntentId);


//                String paymentIntentId = event.getId(); // <-- This is your paymentIntentId
                log.info("PaymentIntent ID: {}", paymentIntentId);

                Optional<Payment> payment=paymentRepository.findByPaymentIntentId(paymentIntentId);
                payment.ifPresent((data)->{
                    data.setPaymentStatus(PaymentStatus.PAID);
                    paymentRepository.save(data);

                    Optional<Subscription> currSubscription=subscriptionRepository.findByCompanyIdAndStatus(data.getCompanyId(),SubscriptionEnum.ACTIVE);
                    currSubscription.ifPresent((currSubs)->{
                        subscriptionRepository.delete(currSubs);
                    });

                    Optional<Subscription> upcomingSubscription=subscriptionRepository.findByCompanyIdAndStatus(data.getCompanyId(),SubscriptionEnum.PENDING);
                    upcomingSubscription.ifPresent(subsc->{
                        subsc.setStatus(SubscriptionEnum.ACTIVE);
//                        int affectedUser=userActivationService.updateActiveUsersBySubscription(subsc.getCompanyId(),subsc.getPerson());
//                        log.info("{} Users Status activated",affectedUser);
                        subscriptionRepository.save(subsc);
                    });
                });


        }
        // Handle invoice.payment_failed
        else if ("invoice.payment_failed".equals(event.getType())) {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (invoice != null) {
                String customerId = invoice.getCustomer();
                String subscriptionId = invoice.getSubscription();
                // TODO: Mark subscription as past_due, notify customer, retry logic, etc.
                Optional<CustomerStripeDetails> optionalStripeDetail=customerStripeDetailsRepository.findByCustomerId(customerId);
                if(optionalStripeDetail.isPresent()){
                    Optional<Subscription> optionalSubscription=subscriptionRepository.findByCompanyIdAndStatus(optionalStripeDetail.get().getCompanyId(), SubscriptionEnum.PENDING);
                    List<Payment> paymentList=paymentRepository.findByCompanyIdAndPaymentStatusOrderByTransactionDateDesc(optionalStripeDetail.get().getCompanyId(), PaymentStatus.PENDING);
                    if(optionalSubscription.isPresent()){
                        Subscription subscription=optionalSubscription.get();
                        subscription.setStatus(SubscriptionEnum.EXPIRED);
                        log.info("Webhook Subscription Failed And Subscription for Company {} is Changed to EXPIRED",subscription.getCompanyId());
                        subscriptionRepository.save(subscription);
                    }
                    if(!paymentList.isEmpty()){
                        Payment myPayment=paymentList.get(0);
                        myPayment.setPaymentStatus(PaymentStatus.FAILED);

                        paymentRepository.save(myPayment);
                        log.info("Webhook Subscription Failed And Payment for Company {} is Changed to FAILED",myPayment.getCompanyId());
                    }
                }
            }
        }

        return ResponseEntity.ok("Received");
    }
}