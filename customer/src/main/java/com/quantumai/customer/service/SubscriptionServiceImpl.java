package com.quantumai.customer.service;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import com.quantumai.customer.repository.PaymentRepository;
import com.quantumai.customer.repository.PlansRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

  @Autowired private SubscriptionRepository subscriptionRepository;

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private PlansRepository plansRepository;

  @Autowired private PaymentService paymentService;

  private ModelMapper modelMapper = new ModelMapper();

  @Override
  public void addSubscription(SubscriptionDTO subscriptionDTO) {
    Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
    subscription.setSubscriptionDate(LocalDate.now());
    //    List<Subscription> mySubscription =
    //        subscriptionRepository.findByCompanyId(subscriptionDTO.getCompanyId());
    //    if (mySubscription.isPresent()) {
    //      subscription.setId(mySubscription.get().getId());
    //    }

    subscriptionRepository.save(subscription);
  }

  @Override
  public void updateSubscription(SubscriptionDTO subscriptionDTO) {
    Subscription subscription = modelMapper.map(subscriptionDTO, Subscription.class);
    subscription.setSubscriptionDate(LocalDate.now());
    subscriptionRepository.save(subscription);
  }

  @Override
  public void isExpired() {
    List<Subscription> subscriptionList = subscriptionRepository.findAll();
    subscriptionList.stream()
        .forEach(
            (subs) -> {
              LocalDate planRenewDate = subs.getSubscriptionDate();
              LocalDate planExpiredDate = planRenewDate.plusMonths(1);
              System.out.println(
                  "Expired-->" + planRenewDate + " planExpiredDate->" + planExpiredDate);
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
    Collections.sort(allInvoice, Comparator.comparing(Payment::getTransactionDate).reversed());
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
        subscriptionRepository.findByCompanyIdAndStatus(companyId, "ACTIVE");
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
        subscriptionRepository.findByCompanyIdAndStatus(companyId, "UPCOMING");
    if (subscription.isPresent()) {
      paymentService.cancelUpcomingSubscriptionStripe(companyId, companyName, email);
    }
  }

  @Override
  public void startUpcomingSubscription(Long companyId, String companyName, String email)
      throws Exception {
    Optional<Subscription> subscription =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, "UPCOMING");
    if (subscription.isPresent()) {
      paymentService.startUpcomingSubscriptionStripe(companyId, companyName, email);
    }
  }
}
