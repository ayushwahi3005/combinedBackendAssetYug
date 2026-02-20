package com.quantumai.customer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.SubscriptionPlan;
import com.quantumai.customer.service.PaymentService;
import com.quantumai.customer.service.StripeService;
import com.quantumai.customer.service.SubscriptionService;
import com.stripe.exception.StripeException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
public class PaymentAPI {

  @Autowired private SubscriptionService subscriptionService;
  private final PaymentService paymentService;

  @Autowired private StripeService stripeService;

  public PaymentAPI(PaymentService paymentService, StripeService stripeService) {
    this.paymentService = paymentService;
    this.stripeService = stripeService;
  }

  @PostMapping("/create-intent")

  public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody JsonNode obj) {
    System.out.println("===>Create Intent");
    System.out.println(obj.get("amount").asLong());
    System.out.println(obj.get("currency").asText());
    System.out.println(obj.get("amount").asLong());
    System.out.println(obj.get("amount").asLong());
    try {
      String clientSecret =
          paymentService.createPaymentIntent(
              obj.get("amount").asLong(),
              obj.get("currency").asText(),
              obj.get("name").asText(),
              obj.get("email").asText());
      Map<String, String> response = new HashMap<>();
      response.put("clientSecret", clientSecret);
      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Collections.singletonMap("error", e.getMessage()));
    }
  }

  @PostMapping("/create-subscription")

  public ResponseEntity<Map<String, String>> createSubscription(@RequestBody JsonNode obj)
      throws Exception {
    //    System.out.println("===>Create subscription"+obj.get("subscriptionPlan").toString());
    try {
      SubscriptionPlan plan;
      String planString = obj.get("subscriptionPlan").asText().trim();
      if (planString.equalsIgnoreCase("MONTHLY")) {

        plan = SubscriptionPlan.MONTHLY;
        System.out.println(
            "===>Create subscription" + planString.equalsIgnoreCase("MONTHLY") + " " + plan);
      } else {
        //        System.out.println("===>Create subscription"+planString);
        plan = SubscriptionPlan.ANNUAL;
        System.out.println(
            "===>Create subscription" + planString.equalsIgnoreCase("ANNUAL") + " " + plan);
      }
      //       = SubscriptionPlan.valueOf(obj.get("subscriptionPlan").toString().toUpperCase())
      com.stripe.model.Subscription stripeSubscription =
          paymentService.createSubscription(
              Long.parseLong(obj.get("companyId").asText()),
              obj.get("paymentMethodId").asText(),
              obj.get("name").asText(),
              obj.get("email").asText(),
              plan,
              obj.get("quantity").asLong(),
              obj.get("amount").asDouble(),
              obj.get("cardHolderName").asText(),
                  obj.get("currentPlanName").asText());
      Map<String, String> response = new HashMap<>();
      //      response.put("clientSecret", clientSecret);
      //      System.out.println("---Subscription-->>>>>>>>"+stripeSubscription.getCustomer());
      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Collections.singletonMap("error", e.getMessage()));
    }
  }

  @PostMapping("/add")

  public void addPayment(@RequestBody Payment payment) {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace(); // Handle the exception if needed
    }
    subscriptionService.addPayment(payment);
  }

  @PutMapping("/update")

  public void updatePayment(@RequestBody Payment Payment) {

    subscriptionService.updatePayment(Payment);
  }

  @GetMapping("/get-invoices/{companyId}")

  public List<Payment> getAllInvoice(@PathVariable Long companyId) {

    return subscriptionService.getAllPayment(companyId);
  }

  @GetMapping("/checkCred")

  public ResponseEntity<Boolean> checkCred() {

    return ResponseEntity.ok(true);
  }

  @PostMapping("/stripe-save-card")

  public void saveCard(@RequestBody Map<String, String> request) throws StripeException {
    String paymentMethodId = request.get("paymentMethodId");
    String customerEmail = request.get("email");
    String cardholderName = request.get("cardholderName");
    Long companyId = Long.parseLong(request.get("companyId"));
    System.out.println(
        paymentMethodId + " " + customerEmail + " " + cardholderName + " " + companyId);
    stripeService.saveCard(paymentMethodId, customerEmail, cardholderName, companyId);
  }

  @GetMapping("/stripe-get-cards/{customerId}")

  public List<Map<String, Object>> getCustomerCards(@PathVariable Long customerId)
      throws StripeException {
    return stripeService.getCustomerCards(customerId);
  }

  @DeleteMapping("/stripe-delete-cards/{paymentMethodId}")

  public void removeCard(@PathVariable String paymentMethodId) throws StripeException {
    System.out.println(paymentMethodId);
    //    String paymentMethodId = request.get("paymentMethodId");

    stripeService.removeCard(paymentMethodId);
  }
  @GetMapping("/payment-intent/{paymentIntentId}")

  public ResponseEntity<Map<String, Object>> getPaymentIntentDetails(
          @PathVariable String paymentIntentId) {
    try {
      Map<String, Object> response = stripeService.getCardLast4Digits(paymentIntentId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.status(500).body(errorResponse);
    }
  }
}
