package com.quantumai.customer.service;

import com.quantumai.customer.entity.CustomerStripeDetails;
import com.quantumai.customer.entity.SubscriptionPlan;
import com.quantumai.customer.repository.CustomerStripeDetailsRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerListParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SubscriptionCreateParams;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StripeService {

  @Value("${stripe.secret.key}")
  private String secretKey;

  @Autowired private CustomerStripeDetailsRepository customerStripeDetailsRepository;

  @Autowired private SubscriptionRepository subscriptionRepository;

  @PostConstruct
  public void init() {
    if (secretKey == null || secretKey.isEmpty()) {
      throw new IllegalStateException("Stripe Secret Key is not set!");
    }
    Stripe.apiKey = secretKey;
  }

  public void saveCard(
      String paymentMethodId, String customerEmail, String cardholderName, Long companyId)
      throws StripeException {
    Stripe.apiKey = secretKey;

    Customer customer = findCustomerByEmail(customerEmail); // ✅ Check for existing customer
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

    Subscription subscription = createSubscription(customer.getId(), companyId);

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

  public List<Map<String, Object>> getCustomerCards(Long customerId) throws StripeException {
    Stripe.apiKey = secretKey;
    Optional<CustomerStripeDetails> customerStripeDetails =
        customerStripeDetailsRepository.findByCompanyId(customerId);
    if (customerStripeDetails.isEmpty()) return null;
    // ✅ 1️⃣ Fetch saved cards for the customer
    PaymentMethodListParams params =
        PaymentMethodListParams.builder()
            .setCustomer(customerStripeDetails.get().getCustomerId())
            .setType(PaymentMethodListParams.Type.CARD)
            .build();

    List<PaymentMethod> paymentMethods = PaymentMethod.list(params).getData();

    // ✅ 2️⃣ Explicitly define the map to avoid type inference issues
    return paymentMethods.stream()
        .map(
            pm -> {
              Map<String, Object> cardDetails = new HashMap<>();
              cardDetails.put("id", pm.getId());
              cardDetails.put("brand", pm.getCard().getBrand());
              cardDetails.put("last4", pm.getCard().getLast4());
              cardDetails.put("expMonth", pm.getCard().getExpMonth());
              cardDetails.put("expYear", pm.getCard().getExpYear());
              return cardDetails;
            })
        .collect(Collectors.toList());
  }

  public void removeCard(String paymentMethodId) throws StripeException {
    Stripe.apiKey = secretKey;

    // ✅ Retrieve PaymentMethod
    PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

    // ✅ Detach from Customer
    paymentMethod.detach();
    Optional<CustomerStripeDetails> customerStripeDetails =
        customerStripeDetailsRepository.findByPaymentMethodId(paymentMethodId);
    if (customerStripeDetails.isPresent()) {
      CustomerStripeDetails myCustomerStripeDetails = customerStripeDetails.get();
      myCustomerStripeDetails.setPaymentMethodId(null);
      customerStripeDetailsRepository.save(myCustomerStripeDetails);
    }

    System.out.println("Card removed successfully from customer ID: " + paymentMethodId);
  }

  public Customer findCustomerByEmail(String email) throws StripeException {
    String startingAfter = null;

    while (true) { // ✅ Loop through all customers using pagination
      CustomerListParams.Builder paramsBuilder = CustomerListParams.builder().setLimit(100L);
      if (startingAfter != null) {
        paramsBuilder.setStartingAfter(startingAfter);
      }

      List<Customer> customers = Customer.list(paramsBuilder.build()).getData();

      for (Customer customer : customers) {
        if (customer.getEmail().equalsIgnoreCase(email)) {
          return customer; // ✅ Found the customer, return it
        }
      }

      if (customers.size() < 100) {
        break; // ✅ No more customers left, stop the loop
      }

      startingAfter = customers.get(customers.size() - 1).getId(); // ✅ Move to the next page
    }

    return null; // ❌ No customer found
  }

  private Subscription createSubscription(String customerId, Long companyId)
      throws StripeException {
    Optional<com.quantumai.customer.entity.Subscription> subscriptionOptional =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, "ACTIVE");

    if (subscriptionOptional.isPresent()) {
      ZonedDateTime nextBillingDate;
      String priceAmountTag;
      SubscriptionPlan plan = subscriptionOptional.get().getSubscriptionPlan();
      LocalDate expiryDate = subscriptionOptional.get().getExpiryDate();

      // ✅ Convert `LocalDate` to `ZonedDateTime`
      nextBillingDate =
          ZonedDateTime.of(
              expiryDate.getYear(),
              expiryDate.getMonthValue(),
              expiryDate.getDayOfMonth(), // Year-Month-Day
              0,
              0,
              0,
              0, // Hours-Minutes-Seconds-Nanoseconds
              ZoneId.of("UTC") // Time Zone
              );

      if (plan == SubscriptionPlan.MONTHLY) {
        priceAmountTag = "price_1Qs970DbrtjFAyfvny0ecIQz";
      } else {
        priceAmountTag = "price_1Qs9QyDbrtjFAyfvQpwzPUAI";
      }

      // ✅ Ensure `trial_end` does not exceed Stripe's 5-year restriction
      long trialEndTimestamp = nextBillingDate.toEpochSecond();
      long maxAllowedTimestamp = ZonedDateTime.now(ZoneId.of("UTC")).plusYears(5).toEpochSecond();
      if (trialEndTimestamp > maxAllowedTimestamp) {
        trialEndTimestamp = maxAllowedTimestamp;
      }

      SubscriptionCreateParams params =
          SubscriptionCreateParams.builder()
              .setCustomer(customerId)
              .addItem(
                  SubscriptionCreateParams.Item.builder()
                      .setPrice(priceAmountTag)
                      .setQuantity(subscriptionOptional.get().getPerson().longValue())
                      .build())
              .setTrialEnd(trialEndTimestamp) // ✅ Billing starts exactly on expiry date
              .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
              .addExpand("latest_invoice.payment_intent")
              .build();

      return Subscription.create(params);
    }
    return null;
  }
}
