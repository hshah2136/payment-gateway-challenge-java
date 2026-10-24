package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class BankClientTest {

  private static final String BANK_URL = "http://bank/payments";

  private MockRestServiceServer bankServer;
  private BankClient bankClient;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    bankServer = MockRestServiceServer.createServer(restTemplate);
    bankClient = new BankClient(restTemplate, BANK_URL);
  }

  @Test
  void whenBankAuthorizesThenResponseIsReturned() {
    bankServer.expect(requestTo(BANK_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"authorized\": true, \"authorization_code\": \"abc\"}",
            MediaType.APPLICATION_JSON));

    BankPaymentResponse response = bankClient.processPayment(bankRequest());

    assertTrue(response.isAuthorized());
    assertEquals("abc", response.getAuthorizationCode());
    bankServer.verify();
  }

  @Test
  void whenBankReturnsServiceUnavailableThenBankUnavailableExceptionIsThrown() {
    bankServer.expect(requestTo(BANK_URL))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThrows(BankUnavailableException.class, () -> bankClient.processPayment(bankRequest()));
  }

  @Test
  void whenBankReturnsEmptyBodyThenBankUnavailableExceptionIsThrown() {
    bankServer.expect(requestTo(BANK_URL))
        .andRespond(withSuccess());

    assertThrows(BankUnavailableException.class, () -> bankClient.processPayment(bankRequest()));
  }

  private BankPaymentRequest bankRequest() {
    return new BankPaymentRequest("2222405343248877", 4, 2030, "GBP", 100, "123");
  }
}
