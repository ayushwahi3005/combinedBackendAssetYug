package com.quantumai.customer.service;
import com.stripe.model.PaymentIntent;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final StripeService stripeService;

    public PaymentService(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    public String createPaymentIntent(Long amount, String currency) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)  // Amount in the smallest currency unit (e.g., cents)
                .setCurrency(currency)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret(); // Return the client secret
    }
}