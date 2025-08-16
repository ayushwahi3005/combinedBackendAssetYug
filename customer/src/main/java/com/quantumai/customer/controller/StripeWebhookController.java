package com.quantumai.customer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.CustomerStripeDetailsRepository;
import com.quantumai.customer.repository.PaymentRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/stripe")
@Slf4j
public class StripeWebhookController {

    // Must not be static; Spring does not inject into static fields.
//    @Value("${stripe.webhook.endpoint}")
    private final String endpointSecret="whsec_d7021a6b2f9313cc9ea4abde0a34eb4c80cdec58398f187b4049681f475a7272";

    @Autowired
    private CustomerStripeDetailsRepository customerStripeDetailsRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired private PaymentRepository paymentRepository;

    @Transactional
    @PostMapping("/webhook")
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
            log.info("------------------------>Inside Payment Succeeded");
            log.info("------------------------>Object {}",event.getObject());
//            log.info("------------------------>Data {}",event.getData().toString());
//            try {
//                ObjectMapper mapper = new ObjectMapper();
//                JsonNode root = mapper.readTree(payload);
//                JsonNode dataObject = root.path("data").path("object");
//
//                String paymentIntentId = dataObject.path("payment_intent").asText(null);
//                String subscriptionId = dataObject.path("subscription").asText(null);
//
//                log.info("Payment Intent ID: {}" , paymentIntentId);
//                log.info("Subscription ID: {}", subscriptionId);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                // Handle JSON parse error
//            }
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
                        Optional<Subscription> optionalSubscription=subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                        Optional <Payment> optionalPayment=paymentRepository.findByPaymentIntentId(paymentIntent);
                        if(optionalSubscription.isPresent()){
                            Subscription subscription=optionalSubscription.get();
                            subscription.setStatus(SubscriptionEnum.ACTIVE);

                            subscriptionRepository.save(subscription);
                            log.info("Webhook Subscription Created And Subscription for Company {} is Changed to Active",subscription.getCompanyId());
                        }
                        if(optionalPayment.isPresent()){
                            Payment myPayment=optionalPayment.get();
                            myPayment.setPaymentStatus(PaymentStatus.PAID);

                            paymentRepository.save(myPayment);
                            log.info("Webhook Subscription Created And Payment for Company {} is Changed to Paid",myPayment.getCompanyId());
                        }


                } else if ("subscription_cycle".equals(billingReason)) {
                    // RENEWAL payment for existing subscription
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
                        Optional<Subscription> optionalSubscription=subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                        Optional <Payment> optionalPayment=paymentRepository.findByPaymentIntentId(paymentIntent);
                        if(optionalSubscription.isPresent()){
                            Subscription subscription=optionalSubscription.get();
                            subscription.setStatus(SubscriptionEnum.ACTIVE);

                            subscriptionRepository.save(subscription);
                            log.info("Webhook Subscription Billing Updated And Subscription for Company {} is Changed to Active",subscription.getCompanyId());
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

        // Handle invoice.payment_failed
        if ("invoice.payment_failed".equals(event.getType())) {
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