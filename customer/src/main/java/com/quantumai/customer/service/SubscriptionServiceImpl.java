package com.quantumai.customer.service;

import com.quantumai.customer.dto.MrrDTO;
import com.quantumai.customer.dto.RevenueTrendDTO;
import com.quantumai.customer.dto.SubscriptionAnalyticsDTO;
import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import com.quantumai.customer.repository.PaymentRepository;
import com.quantumai.customer.repository.PlansRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.SubscriptionRepositoryCustom;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

  @Autowired private SubscriptionRepository subscriptionRepository;
  
  @Autowired
  @Qualifier("subscriptionRepositoryImpl")
  private SubscriptionRepositoryCustom subscriptionRepositoryCustom;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private PlansRepository plansRepository;

  @Autowired private PaymentService paymentService;

  @Autowired private SubscriptionAnalyticsService subscriptionAnalyticsService;
  
//  @Autowired private UserActivationService userActivationService;

  private ModelMapper modelMapper = new ModelMapper();

  @Override
  @Transactional
  public void addSubscription(SubscriptionDTO subscriptionDTO) {
    Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
    subscription.setSubscriptionDate(LocalDate.now());
    if(subscriptionDTO.getSubscriptionName().trim().isEmpty() || subscriptionDTO.getSubscriptionName().trim().isBlank()){
      subscription.setSubscriptionName("Growth");
    }
    // Save the subscription first
    subscription = subscriptionRepository.save(subscription);

    // Update user activations based on the new subscription limit
//    if (subscription.getStatus() == SubscriptionEnum.ACTIVE) {
//      int deactivated = userActivationService.updateActiveUsersBySubscription(
//          subscription.getCompanyId(),
//          subscription.getPerson()
//      );
//
//      if (deactivated > 0) {
//        log.warn("Deactivated {} users for company {} to comply with new subscription limit of {}",
//            deactivated, subscription.getCompanyId(), subscription.getPerson());
//      }
//    }
  }

  @Override
  @Transactional
  public void updateSubscription(SubscriptionDTO subscriptionDTO) {
    // Get the existing subscription to check for person limit changes
    Optional<Subscription> existingOpt = subscriptionRepository.findById(subscriptionDTO.getId());
    if(subscriptionDTO.getSubscriptionName().trim().isBlank()||subscriptionDTO.getSubscriptionName().trim().isEmpty()){
      subscriptionDTO.setSubscriptionName("Growth");
    }
    Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
    subscription.setSubscriptionDate(LocalDate.now());
    subscription = subscriptionRepository.save(subscription);

    // If this is an active subscription and the person limit has changed
//    if (subscription.getStatus() == SubscriptionEnum.ACTIVE &&
//        existingOpt.isPresent() &&
//        existingOpt.get().getPerson() != subscription.getPerson()) {
//
//      int deactivated = userActivationService.updateActiveUsersBySubscription(
//          subscription.getCompanyId(),
//          subscription.getPerson()
//      );
//
//      if (deactivated > 0) {
//        log.warn("Deactivated {} users for company {} to comply with updated subscription limit of {}",
//            deactivated, subscription.getCompanyId(), subscription.getPerson());
//      }
//    }
  }

  @Override
  public void isExpired() {
    List<Subscription> subscriptionList = subscriptionRepository.findByStatus(SubscriptionEnum.ACTIVE);
    subscriptionList.stream()
        .forEach(
            (subs) -> {
              // LocalDate planRenewDate = subs.getSubscriptionDate();
              LocalDate planExpiredDate = subs.getExpiryDate();
              System.out.println(
                  "Expired-->" + planExpiredDate);
              if (LocalDate.now().isAfter(planExpiredDate)
                  || LocalDateTime.now().equals(planExpiredDate)) {
                System.out.println("Yesss");
                subs.setStatus(SubscriptionEnum.EXPIRED);
                subscriptionRepository.save(subs);
              }
            });
  }

  @Override
  public void addPayment(Payment payment) {
    paymentRepository.save(payment);
  }

  @Override
  public void updatePayment(Payment payment) {
    paymentRepository.save(payment);
  }

  @Override
  public List<Payment> getAllPayment(Long companyId) {
    List<Payment> allInvoice = paymentRepository.findByCompanyId(companyId);

    allInvoice.forEach(payment -> {
      try {
        if (payment.getChargeId() != null) {
          Charge charge = Charge.retrieve(payment.getChargeId());
          Charge.PaymentMethodDetails details = charge.getPaymentMethodDetails();

          if (details != null && details.getCard() != null) {
            String last4 = details.getCard().getLast4();
            if (last4 != null) {
              payment.setLast4digit(Long.parseLong(last4));
            }
          }

        } else if (payment.getPaymentIntentId() != null) {
          PaymentIntent intent = PaymentIntent.retrieve(payment.getPaymentIntentId());
          String pmId = intent.getPaymentMethod();

          if (pmId != null) {
            PaymentMethod pm = PaymentMethod.retrieve(pmId);
            if (pm.getCard() != null) {
              payment.setLast4digit(Long.parseLong(pm.getCard().getLast4()));
            }
          }
        }
      } catch (StripeException e) {
        // Log and continue — don't fail the whole list for one bad record
        log.warn("Failed to fetch last4 from Stripe for paymentId={}, chargeId={}: {}",
                payment.getPaymentId(), payment.getChargeId(), e.getMessage());
      }
    });

    allInvoice.sort(Comparator.comparing(Payment::getTransactionDate).reversed());
    return allInvoice;
  }

  @Override
  public void addPlan(Plans plans) {
    plansRepository.save(plans);
  }

  @Override
  public void updatePlan(Plans plans) {
    plansRepository.save(plans);
  }

  @Override
  public void deletePlan(String id) {
    Optional<Plans> optionalPLan = plansRepository.getById(id);
    if (optionalPLan.isPresent()) {
      plansRepository.delete(optionalPLan.get());
    }
  }

  @Override
  public Plans getPlan(String id) {
    Optional<Plans> optionalPLan = plansRepository.getById(id);
    if (optionalPLan.isPresent()) {
      return optionalPLan.get();
    } else {
      return null;
    }
  }

  @Override
  public List<Plans> getAllPlan() {
    return plansRepository.findAll();
  }

  @Override
  public Subscription getCurrentSubscription(Long companyId) {
    Optional<Subscription> subscription =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    return subscription.orElse(null);
  }

  @Override
  public List<Subscription> getAllSubscription(Long companyId) {
    List<Subscription> subscription = subscriptionRepository.findByCompanyId(companyId);
    return subscription;
  }

  @Override
  public void deleteUpcomingSubscription(Long companyId, String companyName, String email)
      throws Exception {
    Optional<Subscription> subscription =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.UPCOMING);
    if (subscription.isPresent()) {
      paymentService.cancelUpcomingSubscriptionStripe(companyId, companyName, email);
    }
  }

  @Override
  public void startUpcomingSubscription(Long companyId, String companyName, String email)
      throws Exception {
    Optional<Subscription> subscription =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.UPCOMING);
    if (subscription.isPresent()) {
      paymentService.startUpcomingSubscriptionStripe(companyId, companyName, email);
    }
  }

  @Override
  public List<SubscriptionGrowthDTO> getSubscriptionGrowth(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start date cannot be after end date");
    }
    
    return subscriptionRepositoryCustom.getSubscriptionGrowth(start, end);
  }
  
  @Override
  public List<SubscriptionAnalyticsDTO> getSubscriptionAnalytics(LocalDate start, LocalDate end, 
      SubscriptionAnalyticsDTO.TimePeriod period) {
    if (start == null || end == null || period == null) {
      throw new IllegalArgumentException("Start date, end date, and period cannot be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start date cannot be after end date");
    }
    
    return subscriptionRepositoryCustom.getSubscriptionAnalytics(start, end, period);
  }
  
  @Override
  public double calculateChurnRate(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start date cannot be after end date");
    }
    
    return subscriptionRepositoryCustom.calculateChurnRate(start, end);
  }
  
  @Override
  public List<RevenueTrendDTO> getRevenueTrends(LocalDate start, LocalDate end, 
      SubscriptionAnalyticsDTO.TimePeriod period) {
    if (start == null || end == null || period == null) {
      throw new IllegalArgumentException("Start date, end date, and period cannot be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start date cannot be after end date");
    }
    
    return subscriptionRepositoryCustom.getRevenueTrends(start, end, period);
  }
  
  @Override
  public List<MrrDTO> getMrrTrend(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start date cannot be after end date");
    }
    
    return subscriptionRepositoryCustom.getMrrTrend(start, end);
  }
}
