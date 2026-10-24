package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;
  @Mock
  private BankClient bankClient;

  private PaymentGatewayService service;

  private static final int EXPIRY_YEAR = YearMonth.now().getYear() + 1;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, new PaymentValidationService(), bankClient);
  }

  @Test
  void whenProcessingPaymentThenCorrectDetailsAreSentToBank() {
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(true);
    when(bankClient.processPayment(any())).thenReturn(bankResponse);

    service.processPayment(validRequest());

    ArgumentCaptor<BankPaymentRequest> captor = ArgumentCaptor.forClass(BankPaymentRequest.class);
    verify(bankClient).processPayment(captor.capture());
    BankPaymentRequest sentToBank = captor.getValue();

    assertEquals("2222405343248877", sentToBank.getCardNumber());
    assertEquals("04/" + EXPIRY_YEAR, sentToBank.getExpiryDate());
    assertEquals("GBP", sentToBank.getCurrency());
    assertEquals(100, sentToBank.getAmount());
    assertEquals("123", sentToBank.getCvv());
  }

  @Test
  void whenBankUnavailableThenPaymentNotStored() {
    when(bankClient.processPayment(any())).thenThrow(new BankUnavailableException("Bank request failed"));

    assertThrows(BankUnavailableException.class, () -> service.processPayment(validRequest()));
    verify(paymentsRepository, never()).add(any());
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(EXPIRY_YEAR);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

}
