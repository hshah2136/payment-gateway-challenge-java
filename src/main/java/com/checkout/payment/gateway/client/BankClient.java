package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private final String bankUrl;
  private final RestTemplate restTemplate;

  public BankClient(RestTemplate restTemplate, @Value("${bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  public BankPaymentResponse processPayment(BankPaymentRequest request) {
    try {
      BankPaymentResponse response = restTemplate.postForObject(bankUrl, request,
          BankPaymentResponse.class);
      if (response == null || response.getAuthorized() == null) {
        throw new BankUnavailableException("Empty or invalid response from bank");
      }
      return response;
    } catch (RestClientException ex) {
      throw new BankUnavailableException("Bank request failed", ex);
    }
  }
}
