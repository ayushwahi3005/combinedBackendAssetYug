package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


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
@Tag(name = "Payment", description = "Payment Management API")
public class PaymentAPI {

  @Autowired private SubscriptionService subscriptionService;
  private final PaymentService paymentService;

  @Autowired private StripeService stripeService;

  public PaymentAPI(PaymentService paymentService, StripeService stripeService) {
    this.paymentService = paymentService;
    this.stripeService = stripeService;
  }

  @Operation(summary = "Create Payment Intent", description = "Endpoint to create payment intent")
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

  @Operation(summary = "Create Subscription", description = "Endpoint to create subscription - Cannot create if PENDING subscription exists")
  @PostMapping("/create-subscription")
  public ResponseEntity<Map<String, String>> createSubscription(@RequestBody JsonNode obj) {
    try {
      SubscriptionPlan plan;
      String planString = obj.get("subscriptionPlan").asText().trim();
      if (planString.equalsIgnoreCase("MONTHLY")) {
        plan = SubscriptionPlan.MONTHLY;
        System.out.println(
            "===>Create subscription" + planString.equalsIgnoreCase("MONTHLY") + " " + plan);
      } else {
        plan = SubscriptionPlan.ANNUAL;
        System.out.println(
            "===>Create subscription" + planString.equalsIgnoreCase("ANNUAL") + " " + plan);
      }
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
      response.put("message", "Subscription created successfully");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
  }

  @Operation(summary = "Add Payment", description = "Endpoint to add payment")
  @PostMapping("/add")

  public void addPayment(@RequestBody Payment payment) {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace(); // Handle the exception if needed
    }
    subscriptionService.addPayment(payment);
  }

  @Operation(summary = "Update Payment", description = "Endpoint to update payment")
  @PutMapping("/update")

  public void updatePayment(@RequestBody Payment Payment) {

    subscriptionService.updatePayment(Payment);
  }

  @Operation(summary = "Get All Invoice", description = "Endpoint to get all invoice")
  @GetMapping("/get-invoices/{companyId}")

  public List<Payment> getAllInvoice(@PathVariable Long companyId) {

    return subscriptionService.getAllPayment(companyId);
  }

  @Operation(summary = "Check Cred", description = "Endpoint to check cred")
  @GetMapping("/checkCred")

  public ResponseEntity<Boolean> checkCred() {

    return ResponseEntity.ok(true);
  }

  @Operation(summary = "Save Card", description = "Endpoint to save card - Only one card allowed per company")
  @PostMapping("/stripe-save-card")
  public ResponseEntity<Map<String, String>> saveCard(@RequestBody Map<String, String> request) {
    try {
      String paymentMethodId = request.get("paymentMethodId");
      String customerEmail = request.get("email");
      String cardholderName = request.get("cardholderName");
      Long companyId = Long.parseLong(request.get("companyId"));
      System.out.println(
          paymentMethodId + " " + customerEmail + " " + cardholderName + " " + companyId);
      paymentService.saveCard(paymentMethodId, customerEmail, cardholderName, companyId);

      Map<String, String> response = new HashMap<>();
      response.put("message", "Card saved successfully");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
  }

  @Operation(summary = "Get Customer Cards", description = "Endpoint to get customer cards")
  @GetMapping("/stripe-get-cards/{customerId}")

  public List<Map<String, Object>> getCustomerCards(@PathVariable Long customerId)
      throws StripeException {
    return stripeService.getCustomerCards(customerId);
  }

  @Operation(summary = "Remove Card", description = "Endpoint to remove card")
  @DeleteMapping("/stripe-delete-cards/{paymentMethodId}")

  public void removeCard(@PathVariable String paymentMethodId) throws StripeException {
    System.out.println(paymentMethodId);
    //    String paymentMethodId = request.get("paymentMethodId");

    stripeService.removeCard(paymentMethodId);
  }
  @Operation(summary = "Get Payment Intent Details", description = "Endpoint to get payment intent details")
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
