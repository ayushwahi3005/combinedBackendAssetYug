package com.quantumai.customer.service;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import com.quantumai.customer.repository.PaymentRepository;
import com.quantumai.customer.repository.PlansRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;


    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PlansRepository plansRepository;

    private ModelMapper modelMapper=new ModelMapper();
    @Override
    public void addSubscription(SubscriptionDTO subscriptionDTO) {
        Subscription subscription=modelMapper.map(subscriptionDTO,Subscription.class);
        subscription.setSubscriptionDate(LocalDate.now());
        Optional<Subscription> mySubscription=subscriptionRepository.findByCompanyId(subscriptionDTO.getCompanyId());
        if(mySubscription.isPresent()){
            subscription.setId(mySubscription.get().getId());
        }


        subscriptionRepository.save(subscription);
    }

    @Override
    public void updateSubscription(SubscriptionDTO subscriptionDTO) {
        Subscription subscription=modelMapper.map(subscriptionDTO,Subscription.class);
        subscription.setSubscriptionDate(LocalDate.now());
        subscriptionRepository.save(subscription);
    }

    @Override
    public void isExpired() {
        List<Subscription> subscriptionList=subscriptionRepository.findAll();
        subscriptionList.stream().forEach((subs)->{
            LocalDate planRenewDate=subs.getSubscriptionDate();
            LocalDate planExpiredDate=planRenewDate.plusMonths(1);
            System.out.println("Expired-->"+planRenewDate+" planExpiredDate->"+planExpiredDate);
            if(LocalDate.now().isBefore(planExpiredDate)||LocalDateTime.now().equals(planExpiredDate)){
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
    public void addPlan(Plans plans) {
        plansRepository.save(plans);
    }

    @Override
    public void updatePlan(Plans plans) {
        plansRepository.save(plans);
    }

    @Override
    public void deletePlan(String id) {
        Optional<Plans> optionalPLan= plansRepository.getById(id);
        if(optionalPLan.isPresent()){
            plansRepository.delete(optionalPLan.get());
        }
    }

    @Override
    public Plans getPlan(String id) {
        Optional<Plans> optionalPLan= plansRepository.getById(id);
        if(optionalPLan.isPresent()){
            return optionalPLan.get();
        }
        else{
            return null;
        }
    }

    @Override
    public List<Plans> getAllPlan() {
        return plansRepository.findAll();
    }

    @Override
    public Subscription getCurrentSubscription(String companyId) {
        Optional<Subscription> subscription=subscriptionRepository.findByCompanyId(companyId);
        if(subscription.isPresent()){
            return subscription.get();
        }
        else{
            return null;
        }

    }
}
