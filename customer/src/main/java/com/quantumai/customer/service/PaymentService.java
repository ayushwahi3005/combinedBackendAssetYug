package com.quantumai.customer.service;

import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.PlanDowngradeException;
import com.quantumai.customer.exception.PlanPersonException;
import com.quantumai.customer.repository.CustomerStripeDetailsRepository;
import com.quantumai.customer.repository.PaymentRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

  private final StripeService stripeService;

  @Value("${stripe.secret.key}")
  private String secretKey;

  @Autowired private SubscriptionRepository subscriptionRepository;

  @Autowired private CustomerStripeDetailsRepository customerStripeDetailsRepository;

  @Autowired private PaymentRepository paymentRepository;

  public PaymentService(StripeService stripeService) {
    this.stripeService = stripeService;
  }

  public String createPaymentIntent(Long amount, String currency, String name, String email)
      throws StripeException {
    String customerId = createCustomerIfNotExists(email, name);
    PaymentIntentCreateParams params =
        PaymentIntentCreateParams.builder()
            .setAmount(amount) // Amount in the smallest currency unit (e.g., cents)
            .setCurrency(currency)
            .build();

    PaymentIntent paymentIntent = PaymentIntent.create(params);
    createInvoice(customerId, amount / 100, currency, "First Subscription");
    return paymentIntent.getClientSecret(); // Return the client secret
  }


  public Subscription createSubscription(
      Long companyId,
      String paymentMethodId,
      String name,
      String email,
      SubscriptionPlan planSelected,
      Long quantity,
      Double amount,
      String cardHolderName)
      throws Exception {
    List<com.quantumai.customer.entity.Subscription> subscriptionList =
        subscriptionRepository.findByCompanyId(companyId);
    String priceAmountTag;

    if (planSelected == SubscriptionPlan.MONTHLY) {
      priceAmountTag = "price_1Qs970DbrtjFAyfvny0ecIQz"; // Monthly plan price ID
    } else {
      priceAmountTag = "price_1Qs9QyDbrtjFAyfvQpwzPUAI"; // Annual plan price ID
    }

    if (!subscriptionList.isEmpty()) {
      Optional<com.quantumai.customer.entity.Subscription> existingSubscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
      if (existingSubscriptionOptional.isEmpty()) {
        throw new Exception("No Active Subscription");
      }
      LocalDate expiryDate = existingSubscriptionOptional.get().getExpiryDate();

      SubscriptionPlan existingPlan = existingSubscriptionOptional.get().getSubscriptionPlan();
      Optional<CustomerStripeDetails> optionalCustomerStripeDetails =
          customerStripeDetailsRepository.findByCompanyId(companyId);
      if (existingPlan == planSelected
          && existingSubscriptionOptional.get().getPerson() == quantity.intValue()) {
        throw new PlanPersonException("Same Plan Cannot be Selected");
      }
      if (existingPlan == SubscriptionPlan.MONTHLY) {
        if (planSelected == SubscriptionPlan.MONTHLY) {

          if (optionalCustomerStripeDetails.isPresent()) {
            List<Subscription> subscriptions =
                Subscription.list(
                        Map.of("customer", optionalCustomerStripeDetails.get().getCustomerId()))
                    .getData();
            String subscriptionId="";
            for (Subscription sub : subscriptions) {
              if (!sub.getStatus()
                  .equals("canceled")) { // Ensure only active subscriptions are updated
                SubscriptionUpdateParams params =
                    SubscriptionUpdateParams.builder()
                        .addItem(
                            SubscriptionUpdateParams.Item.builder()
                                .setId(
                                    sub.getItems()
                                        .getData()
                                        .get(0)
                                        .getId()) // Get current subscription item ID
                                .setPrice(priceAmountTag)
                                .setQuantity(quantity) // Replace with your new Stripe price ID
                                .build())
                        .setProrationBehavior(
                            SubscriptionUpdateParams.ProrationBehavior
                                .NONE) // Optional: Handle proration
                        .build();

                Subscription updatedSubscription = sub.update(params);
                System.out.println("Updated subscription ID: " + updatedSubscription.getId());
                subscriptionId=updatedSubscription.getId();
              }
            }
            com.quantumai.customer.entity.Subscription newSubscription =
                new com.quantumai.customer.entity.Subscription();
            newSubscription.setSubscriptionPlan(planSelected);
            newSubscription.setAmount(amount);
            newSubscription.setStatus(SubscriptionEnum.UPCOMING);
            newSubscription.setSubscriptionDate(expiryDate);
            newSubscription.setExpiryDate(expiryDate.plusMonths(1));
            newSubscription.setCompanyId(companyId);
            newSubscription.setPerson(quantity.intValue());
            newSubscription.setStripeSubscriptionId(subscriptionId);
            newSubscription.setStripeCustomerId(optionalCustomerStripeDetails.get().getCustomerId());
            subscriptionRepository.save(newSubscription);
          }
        } else if (planSelected == SubscriptionPlan.ANNUAL) {
          if (optionalCustomerStripeDetails.isPresent()) {
            List<Subscription> subscriptions =
                    Subscription.list(
                                    Map.of("customer", optionalCustomerStripeDetails.get().getCustomerId()))
                            .getData();
            for (Subscription sub : subscriptions) {
              if (!sub.getStatus().equals("canceled")) {
                System.out.println("Canceling current subscription: " + sub.getId());
                sub.cancel(
                        Map.of("invoice_now", false, "prorate", false)); // Do not charge immediately
              }
            }

            Instant startDate =
                    expiryDate.atStartOfDay(ZoneId.of("UTC")).toInstant(); // Desired start date in UTC
            long trialEndTimestamp = startDate.getEpochSecond();

            String customerId = createCustomerIfNotExists(email, name);
            SubscriptionCreateParams params =
                    SubscriptionCreateParams.builder()
                            .setCustomer(customerId)
                            .addItem(
                                    SubscriptionCreateParams.Item.builder()
                                            .setPrice(priceAmountTag)
                                            .setQuantity(quantity)
                                            .build())
                            //
                            // .setBillingCycleAnchor(expiryDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()) // ✅ Start from expiry date
                            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                            .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE)
                            .addExpand("latest_invoice.payment_intent")
                            .setTrialEnd(trialEndTimestamp)
                            .build();
            Subscription subscription = Subscription.create(params);

            com.quantumai.customer.entity.Subscription newSubscription =
                    new com.quantumai.customer.entity.Subscription();
            newSubscription.setSubscriptionPlan(planSelected);
            newSubscription.setAmount(amount);
            newSubscription.setStatus(SubscriptionEnum.UPCOMING);
            newSubscription.setSubscriptionDate(expiryDate);
            newSubscription.setExpiryDate(expiryDate.plusYears(1));
            newSubscription.setCompanyId(companyId);
            newSubscription.setPerson(quantity.intValue());
            newSubscription.setStripeSubscriptionId(subscription.getId());
            newSubscription.setStripeCustomerId(optionalCustomerStripeDetails.get().getCustomerId());
            subscriptionRepository.save(newSubscription);
          }
        }

      } else {
        if (planSelected == SubscriptionPlan.ANNUAL) {

          if (optionalCustomerStripeDetails.isPresent()) {
            List<Subscription> subscriptions =
                Subscription.list(
                        Map.of("customer", optionalCustomerStripeDetails.get().getCustomerId()))
                    .getData();
            String subscriptionId="";
            for (Subscription sub : subscriptions) {
              if (!sub.getStatus()
                  .equals("canceled")) { // Ensure only active subscriptions are updated
                SubscriptionUpdateParams params =
                    SubscriptionUpdateParams.builder()
                        .addItem(
                            SubscriptionUpdateParams.Item.builder()
                                .setId(
                                    sub.getItems()
                                        .getData()
                                        .get(0)
                                        .getId()) // Get current subscription item ID
                                .setPrice(priceAmountTag)
                                .setQuantity(quantity) // Replace with your new Stripe price ID
                                .build())
                        .setProrationBehavior(
                            SubscriptionUpdateParams.ProrationBehavior
                                .NONE) // Optional: Handle proration
                        .build();

                Subscription updatedSubscription = sub.update(params);
                subscriptionId=updatedSubscription.getId();
                System.out.println("Updated subscription ID: " + updatedSubscription.getId());
              }
            }
            com.quantumai.customer.entity.Subscription newSubscription =
                new com.quantumai.customer.entity.Subscription();
            newSubscription.setSubscriptionPlan(planSelected);
            newSubscription.setAmount(amount);
            newSubscription.setStatus(SubscriptionEnum.UPCOMING);
            newSubscription.setSubscriptionDate(expiryDate);
            newSubscription.setExpiryDate(expiryDate.plusYears(1));
            newSubscription.setCompanyId(companyId);
            newSubscription.setPerson(quantity.intValue());
            newSubscription.setStripeSubscriptionId(subscriptionId);
            newSubscription.setStripeCustomerId(optionalCustomerStripeDetails.get().getCustomerId());
            subscriptionRepository.save(newSubscription);
          }
        } else if (planSelected == SubscriptionPlan.MONTHLY) {
          Instant startDate =
              expiryDate.atStartOfDay(ZoneId.of("UTC")).toInstant(); // Desired start date in UTC
          long trialEndTimestamp = startDate.getEpochSecond();

          String customerId = createCustomerIfNotExists(email, name);
          SubscriptionCreateParams params =
              SubscriptionCreateParams.builder()
                  .setCustomer(customerId)
                  .addItem(
                      SubscriptionCreateParams.Item.builder()
                          .setPrice(priceAmountTag)
                          .setQuantity(quantity)
                          .build())
                  //
                  // .setBillingCycleAnchor(expiryDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()) // ✅ Start from expiry date
                  .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                  .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE)
                  .addExpand("latest_invoice.payment_intent")
                  .setTrialEnd(trialEndTimestamp)
                  .build();
          Subscription subscription = Subscription.create(params);

          com.quantumai.customer.entity.Subscription newSubscription =
              new com.quantumai.customer.entity.Subscription();
          newSubscription.setSubscriptionPlan(planSelected);
          newSubscription.setAmount(amount);
          newSubscription.setStatus(SubscriptionEnum.UPCOMING);
          newSubscription.setSubscriptionDate(expiryDate);
          newSubscription.setExpiryDate(expiryDate.plusMonths(1));
          newSubscription.setCompanyId(companyId);
          newSubscription.setPerson(quantity.intValue());
          newSubscription.setStripeSubscriptionId(subscription.getId());
          newSubscription.setStripeCustomerId(optionalCustomerStripeDetails.get().getCustomerId());
          subscriptionRepository.save(newSubscription);
        }
      }

      return null;
    }

    // If no existing subscription, create a new one immediately
    String customerId = createCustomerIfNotExists(email, name);
    PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
    paymentMethod.attach(Map.of("customer", customerId));

    CustomerUpdateParams customerUpdateParams =
        CustomerUpdateParams.builder()
            .setInvoiceSettings(
                CustomerUpdateParams.InvoiceSettings.builder()
                    .setDefaultPaymentMethod(paymentMethodId)
                    .build())
            .build();
    Customer.retrieve(customerId).update(customerUpdateParams);

    SubscriptionCreateParams params =
        SubscriptionCreateParams.builder()
            .setCustomer(customerId)
            .addItem(
                SubscriptionCreateParams.Item.builder()
                    .setPrice(priceAmountTag)
                    .setQuantity(quantity)
                    .build())
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addExpand("latest_invoice.payment_intent")
            .build();

    Subscription subscription = Subscription.create(params);

    if (subscription.getLatestInvoiceObject().getPaymentIntentObject() != null) {
      String paymentIntentId =
          subscription.getLatestInvoiceObject().getPaymentIntentObject().getId();
      PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
      intent.confirm();

      Payment payment = new Payment();
      payment.setPaymentStatus(PaymentStatus.PENDING);
      payment.setPaymentType(PaymentType.CREDIT_CARD);
      payment.setDescription("Subscription Payment");
      payment.setCompanyId(companyId);
      payment.setAmount(amount);
      payment.setStartDate(LocalDateTime.now());
      if (planSelected == SubscriptionPlan.MONTHLY) {
        payment.setEndDate(LocalDateTime.now().plusMonths(1));
      } else {
        payment.setEndDate(LocalDateTime.now().plusYears(1).minusDays(1));
      }
      payment.setPerson(quantity.intValue());
      payment.setTransactionDate(LocalDateTime.now());
      payment.setPlanSelected(planSelected);
      payment.setPaymentIntentId(paymentIntentId);
      if (intent.getLatestCharge() != null) {
        payment.setChargeId(intent.getLatestChargeObject().getId());
      }

      // Set invoiceId from subscription’s invoice id
      if (subscription.getLatestInvoice() != null) {
        payment.setInvoiceId(subscription.getLatestInvoice());
      }
      paymentRepository.save(payment);

      com.quantumai.customer.entity.Subscription newSubscription =
          new com.quantumai.customer.entity.Subscription();
      newSubscription.setSubscriptionPlan(planSelected);
      newSubscription.setAmount(amount);
//      newSubscription.setStatus(SubscriptionEnum.ACTIVE);
      newSubscription.setStatus(SubscriptionEnum.PENDING);
      newSubscription.setSubscriptionDate(LocalDate.now());
      newSubscription.setExpiryDate(
          planSelected == SubscriptionPlan.MONTHLY
              ? LocalDate.now().plusMonths(1)
              : LocalDate.now().plusYears(1));
      newSubscription.setCompanyId(companyId);
      newSubscription.setPerson(quantity.intValue());
      newSubscription.setStripeSubscriptionId(subscription.getId());
      newSubscription.setStripeCustomerId(customerId);
      subscriptionRepository.save(newSubscription);

      saveCard(paymentMethodId, email, cardHolderName, companyId);
    }

    return subscription;
  }

  public void saveCard(
      String paymentMethodId, String customerEmail, String cardholderName, Long companyId)
      throws StripeException {
    Stripe.apiKey = secretKey;
    // To remove before saved card
    Optional<CustomerStripeDetails> optionalCustomerStripeDetails =
        customerStripeDetailsRepository.findByCompanyId(companyId);
    if (optionalCustomerStripeDetails.isPresent()) {
      stripeService.removeCard(optionalCustomerStripeDetails.get().getPaymentMethodId());
    }

    Customer customer =
        stripeService.findCustomerByEmail(customerEmail); // ✅ Check for existing customer
    String myCustomerId = null;
    if (customer == null) {
      // ✅ If no customer exists, create a new one
      Map<String, Object> customerParams = new HashMap<>();
      customerParams.put("email", customerEmail);
      customerParams.put("name", cardholderName);
      customer = Customer.create(customerParams);
    } else {
      // ✅ If customer exists, update name (optional)
      //      myCustomerId
      //       customerStripeDetailsRepository.save(customerStripeDetails);
      Optional<CustomerStripeDetails> optionalcustomerStripeDetails =
          customerStripeDetailsRepository.findByCompanyId(companyId);
      if (optionalcustomerStripeDetails.isPresent()) {
        myCustomerId = optionalcustomerStripeDetails.get().getId();
      }
      Map<String, Object> updateParams = new HashMap<>();
      updateParams.put("name", cardholderName); // Update name if changed
      customer.update(updateParams);
    }

    // ✅ Attach the new Payment Method to the existing/new Customer
    PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
    paymentMethod.attach(Map.of("customer", customer.getId()));

    // ✅ Update PaymentMethod with latest Cardholder Name
    Map<String, Object> billingDetails = new HashMap<>();
    billingDetails.put("name", cardholderName); // ✅ Add cardholder name
    Map<String, Object> updatePaymentParams = new HashMap<>();
    updatePaymentParams.put("billing_details", billingDetails);
    paymentMethod.update(updatePaymentParams);

    // ✅ Store in MongoDB

    CustomerStripeDetails customerStripeDetails = new CustomerStripeDetails();
    customerStripeDetails.setId(myCustomerId);
    customerStripeDetails.setCompanyId(companyId);
    customerStripeDetails.setCustomerId(customer.getId());
    customerStripeDetails.setPaymentMethodId(paymentMethod.getId());
    customerStripeDetails.setFirstName(cardholderName);
    customerStripeDetails.setEmail(customerEmail);
    customerStripeDetailsRepository.save(customerStripeDetails);

    System.out.println("Card saved successfully for customer ID: " + customer.getId());
  }

  private Invoice createInvoice(String customerId, Long amount, String currency, String description)
      throws StripeException {
    // 1️⃣ Create Invoice Item (Adding the amount to the invoice)
    InvoiceItemCreateParams invoiceItemParams =
        InvoiceItemCreateParams.builder()
            .setCustomer(customerId)
            .setAmount(amount * 100) // Convert to cents
            .setCurrency(currency)
            .setDescription(description)
            .build();
    InvoiceItem.create(invoiceItemParams);

    // 2️⃣ Create the Invoice
    InvoiceCreateParams invoiceParams =
        InvoiceCreateParams.builder()
            .setCustomer(customerId)
            .setAutoAdvance(true) // Automatically finalize invoice
            .build();
    Invoice invoice = Invoice.create(invoiceParams);

    // 3️⃣ Finalize the Invoice (Required to generate invoice PDF)
    return invoice.finalizeInvoice();
  }

  public String createCustomerIfNotExists(String email, String name) throws StripeException {
    String startingAfter = null;

    while (true) { // ✅ Loop through all customers using pagination
      CustomerListParams.Builder paramsBuilder = CustomerListParams.builder().setLimit(100L);
      if (startingAfter != null) {
        paramsBuilder.setStartingAfter(startingAfter);
      }

      List<Customer> customers = Customer.list(paramsBuilder.build()).getData();

      for (Customer customer : customers) {
        if (customer.getEmail().equalsIgnoreCase(email)) {
          return customer.getId(); // ✅ Found the customer, return it
        }
      }

      if (customers.size() < 100) {
        break; // ✅ No more customers left, stop the loop
      }

      startingAfter = customers.get(customers.size() - 1).getId(); // ✅ Move to the next page
    }

    // ❌ No customer found
    CustomerCreateParams customerParams =
        CustomerCreateParams.builder().setEmail(email).setName(name).build();
    Customer customer = Customer.create(customerParams);

    return customer.getId();
  }

  public void cancelUpcomingSubscriptionStripe(Long companyId, String companyName, String email)
      throws Exception {
    List<com.quantumai.customer.entity.Subscription> subscriptionList =
        subscriptionRepository.findByCompanyId(companyId);
    Optional<com.quantumai.customer.entity.Subscription> upcomingPlan =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.UPCOMING);

    String priceAmountTag;
    if (upcomingPlan.isPresent()) {

      Optional<CustomerStripeDetails> optionalCustomerStripeDetails =
          customerStripeDetailsRepository.findByCompanyId(companyId);
      if (optionalCustomerStripeDetails.isPresent()) {
        List<Subscription> subscriptions =
            Subscription.list(
                    Map.of("customer", optionalCustomerStripeDetails.get().getCustomerId()))
                .getData();
        for (Subscription sub : subscriptions) {
          if (!sub.getStatus()
              .equals("canceled")) { // Ensure only active subscriptions are canceled
            sub.cancel(
                Map.of(
                    "invoice_now", false,
                    "prorate", false));
          }
        }

        // Delete Upcoming from database
        subscriptionRepository.delete(upcomingPlan.get());
        Optional<com.quantumai.customer.entity.Subscription> currentPlan =
            subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
        if (currentPlan.isEmpty()) {
          throw new Exception("No Active Subscription");
        }
        if (currentPlan.get().getSubscriptionPlan() == SubscriptionPlan.MONTHLY) {
          priceAmountTag = "price_1Qs970DbrtjFAyfvny0ecIQz"; // Monthly plan price ID
        } else {
          priceAmountTag = "price_1Qs9QyDbrtjFAyfvQpwzPUAI"; // Annual plan price ID
        }

        // Re Subscribe current Subscription
        Instant startDate =
            currentPlan
                .get()
                .getExpiryDate()
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant(); // Desired start date in UTC
        long trialEndTimestamp = startDate.getEpochSecond();
        String customerId = createCustomerIfNotExists(email, companyName);
        SubscriptionCreateParams params =
            SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(priceAmountTag)
                        .setQuantity(currentPlan.get().getPerson().longValue())
                        .build())
                //
                // .setBillingCycleAnchor(expiryDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()) // ✅ Start from expiry date
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE)
                .addExpand("latest_invoice.payment_intent")
                .setTrialEnd(trialEndTimestamp)
                .build();
        Subscription subscription = Subscription.create(params);
      }
    }
  }

  public void startUpcomingSubscriptionStripe(Long companyId, String companyName, String email)
      throws PlanDowngradeException, Exception {
    List<com.quantumai.customer.entity.Subscription> subscriptionList =
        subscriptionRepository.findByCompanyId(companyId);
    Optional<com.quantumai.customer.entity.Subscription> optionalUpcomingPlan =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.UPCOMING);
    Optional<com.quantumai.customer.entity.Subscription> optionalCurrentPlan =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);

    String priceAmountTag;
    if (optionalUpcomingPlan.isPresent()) {

      Optional<CustomerStripeDetails> optionalCustomerStripeDetails =
          customerStripeDetailsRepository.findByCompanyId(companyId);
      if (optionalCustomerStripeDetails.isPresent()) {

        if (optionalCurrentPlan.isPresent()) {
          com.quantumai.customer.entity.Subscription currentSubscription =
              optionalCurrentPlan.get();
          com.quantumai.customer.entity.Subscription upcomingSubscription =
              optionalUpcomingPlan.get();
          System.out.println(
              "=======> Current Person "
                  + currentSubscription.getPerson()
                  + " Upcoming Person "
                  + upcomingSubscription.getPerson());
          if (currentSubscription.getSubscriptionPlan() == SubscriptionPlan.MONTHLY
              && upcomingSubscription.getSubscriptionPlan() == SubscriptionPlan.MONTHLY
              && upcomingSubscription.getPerson() > currentSubscription.getPerson()) {

            String stripeCustomerId = optionalCustomerStripeDetails.get().getCustomerId();

            // Get the saved payment method ID
            String paymentMethodId = optionalCustomerStripeDetails.get().getPaymentMethodId();
            Double amountPerPerson =
                upcomingSubscription.getAmount() / upcomingSubscription.getPerson();
            Long totalAmount =
                (upcomingSubscription.getPerson() - currentSubscription.getPerson())
                    * amountPerPerson.longValue();
            if (stripeCustomerId != null && paymentMethodId != null) {
              // Call function to create payment
              PaymentIntentCreateParams params =
                  PaymentIntentCreateParams.builder()
                      .setCustomer(stripeCustomerId) // Set customer ID
                      .setAmount(totalAmount * 100) // Convert amount to cents
                      .setCurrency("usd") // Change as per your currency
                      .setPaymentMethod(paymentMethodId) // Use the saved card
                      .setConfirm(true) // Confirm payment immediately
                      .setOffSession(
                          true) // Use for subscriptions to charge without user interaction
                      .build();

              PaymentIntent paymentIntent = PaymentIntent.create(params);
              System.out.println("Payment successful. PaymentIntent ID: " + paymentIntent.getId());

              Payment payment = new Payment();
              payment.setPaymentStatus(PaymentStatus.PENDING);
              payment.setPaymentType(PaymentType.CREDIT_CARD);
              payment.setDescription("Subscription Payment");
              payment.setCompanyId(companyId);
              payment.setAmount(totalAmount.doubleValue());
              payment.setPlanSelected(SubscriptionPlan.MONTHLY);
              payment.setStartDate(LocalDateTime.now());
              payment.setEndDate(LocalDateTime.now().plusMonths(1).minusDays(1));

              payment.setPerson(upcomingSubscription.getPerson() - currentSubscription.getPerson());
              payment.setTransactionDate(LocalDateTime.now());

              payment.setPaymentIntentId(paymentIntent.getId());
              if (paymentIntent.getLatestCharge() != null&& paymentIntent.getLatestChargeObject()!=null) {
                payment.setChargeId(paymentIntent.getLatestChargeObject().getId());
              }

              // Set invoiceId from subscription’s invoice id
//              if (subscription.getLatestInvoice() != null) {
//                payment.setInvoiceId(subscription.getLatestInvoice());
//              }
              payment.setInvoiceId(paymentIntent.getInvoice());
              paymentRepository.save(payment);

//              upcomingSubscription.setStatus(SubscriptionEnum.ACTIVE);
              upcomingSubscription.setStatus(SubscriptionEnum.PENDING);
              upcomingSubscription.setStripeCustomerId(currentSubscription.getStripeCustomerId());
//              subscriptionRepository.delete(currentSubscription);
              upcomingSubscription.setSubscriptionDate(currentSubscription.getSubscriptionDate());
              upcomingSubscription.setExpiryDate(currentSubscription.getExpiryDate());
              subscriptionRepository.save(upcomingSubscription);
            } else {
              throw new Exception("Stripe Customer ID or Payment Method ID is missing");
            }
          } else if (currentSubscription.getSubscriptionPlan() == SubscriptionPlan.ANNUAL
              && upcomingSubscription.getSubscriptionPlan() == SubscriptionPlan.ANNUAL
              && upcomingSubscription.getPerson() > currentSubscription.getPerson()) {

            String stripeCustomerId = optionalCustomerStripeDetails.get().getCustomerId();

            // Get the saved payment method ID
            String paymentMethodId = optionalCustomerStripeDetails.get().getPaymentMethodId();
            //            int
            // leftMonths=currentSubscription.getExpiryDate().getMonthValue()-LocalDate.now().getMonthValue();
            long leftMonths =
                ChronoUnit.MONTHS.between(
                    LocalDate.now().withDayOfMonth(1),
                    currentSubscription.getExpiryDate().withDayOfMonth(1));
            long amount =
                (upcomingSubscription.getAmount().longValue() * leftMonths)
                    / (12L * upcomingSubscription.getPerson());
            int increasedPerson =
                upcomingSubscription.getPerson() - currentSubscription.getPerson();
            System.out.println(
                "leftMonths-->"
                    + leftMonths
                    + "Amount---------->"
                    + amount
                    + " increasedPerson--->"
                    + increasedPerson);
            if (stripeCustomerId != null && paymentMethodId != null) {
              // Call function to create payment
              PaymentIntentCreateParams params =
                  PaymentIntentCreateParams.builder()
                      .setCustomer(stripeCustomerId) // Set customer ID
                      .setAmount(amount * 100 * increasedPerson) // Convert amount to cents
                      .setCurrency("usd") // Change as per your currency
                      .setPaymentMethod(paymentMethodId) // Use the saved card
                      .setConfirm(true) // Confirm payment immediately
                      .setOffSession(
                          true) // Use for subscriptions to charge without user interaction
                      .build();

              PaymentIntent paymentIntent = PaymentIntent.create(params);
              System.out.println("Payment successful. PaymentIntent ID: " + paymentIntent.getId());

              Payment payment = new Payment();
              payment.setPaymentStatus(PaymentStatus.PENDING);
              payment.setPaymentType(PaymentType.CREDIT_CARD);
              payment.setDescription("Subscription Payment");
              payment.setCompanyId(companyId);
              payment.setAmount(amount * 1.00 * increasedPerson);
              payment.setPerson(increasedPerson);
              payment.setStartDate(LocalDateTime.now());
              payment.setEndDate(LocalDateTime.now().plusYears(1).minusDays(1));
              payment.setPlanSelected(SubscriptionPlan.ANNUAL);
              payment.setTransactionDate(LocalDateTime.now());
              payment.setPaymentIntentId(paymentIntent.getId());
              if (paymentIntent.getLatestCharge() != null) {
                payment.setChargeId(paymentIntent.getLatestChargeObject().getId());
              }

              // Set invoiceId from subscription’s invoice id

                payment.setInvoiceId(paymentIntent.getInvoice());

              paymentRepository.save(payment);

              upcomingSubscription.setStatus(SubscriptionEnum.ACTIVE);
              upcomingSubscription.setExpiryDate(currentSubscription.getExpiryDate());
              upcomingSubscription.setSubscriptionDate(LocalDate.now());
              upcomingSubscription.setStripeCustomerId(currentSubscription.getStripeCustomerId());
//              subscriptionRepository.delete(currentSubscription);

              subscriptionRepository.save(upcomingSubscription);
            } else {
              throw new Exception("Stripe Customer ID or Payment Method ID is missing");
            }
          } else {
            throw new PlanDowngradeException("Cannot Process Request");
          }
        }
      }
    }
  }
}
