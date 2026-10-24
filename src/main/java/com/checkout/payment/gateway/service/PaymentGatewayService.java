package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.exception.PaymentRejectedException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final PaymentValidationService validationService;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      PaymentValidationService validationService, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.validationService = validationService;
    this.bankClient = bankClient;
  }

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
  }

  public PaymentResponse processPayment(PostPaymentRequest request) {
    if (!validationService.isExpiryDateValid(request.getExpiryMonth(), request.getExpiryYear())) {
      throw new PaymentRejectedException("Expiry date must be in future");
    }

    BankPaymentRequest bankRequest = new BankPaymentRequest(
        request.getCardNumber(),
        request.getExpiryMonth(),
        request.getExpiryYear(),
        request.getCurrency(),
        request.getAmount(),
        request.getCvv()
    );

    BankPaymentResponse bankResponse = bankClient.processPayment(bankRequest);

    PaymentStatus status =
        bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setStatus(status);
    payment.setCardNumberLastFour(
        request.getCardNumber().substring(request.getCardNumber().length() - 4));
    payment.setExpiryMonth(request.getExpiryMonth());
    payment.setExpiryYear(request.getExpiryYear());
    payment.setCurrency(request.getCurrency());
    payment.setAmount(request.getAmount());

    paymentsRepository.add(payment);
    return payment;
  }
}
