package com.checkout.payment.gateway.controller;


import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  PaymentsRepository paymentsRepository;

  @MockBean
  private BankClient bankClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PaymentResponse payment = new PaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour("4321");

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card_number_last_four").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  @Test
  void whenPaymentIdIsNotAUuidThen400IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid payment id"));
  }

  @Test
  void whenCardNumberInvalidThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("card_number", "123");

    assertRejected(postPayment(request), "Card number must be 14-19 digits");
  }

  @Test
  void whenExpiryMonthInvalidThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("expiry_month", 13);

    assertRejected(postPayment(request), "Expiry month must be between 1 and 12");
  }

  @Test
  void whenExpiryYearMissingThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.remove("expiry_year");

    assertRejected(postPayment(request), "Expiry year is required");
  }

  @Test
  void whenCurrencyNotSupportedThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("currency", "JPY");

    assertRejected(postPayment(request), "Currency must be one of GBP, EUR, USD");
  }

  @Test
  void whenAmountNotPositiveThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("amount", 0);

    assertRejected(postPayment(request), "Amount must be a positive integer");
  }

  @Test
  void whenCvvInvalidThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("cvv", "12");

    assertRejected(postPayment(request), "CVV must be 3-4 digits");
  }

  @Test
  void whenMultipleFieldsInvalidThenAllErrorsAreReturned() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("card_number", "123");
    request.put("currency", "JPY");
    request.put("cvv", "12");

    postPayment(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.errors", containsInAnyOrder(
            "Card number must be 14-19 digits",
            "Currency must be one of GBP, EUR, USD",
            "CVV must be 3-4 digits")));
  }

  @Test
  void whenCardIsExpiredThenPaymentIsRejected() throws Exception {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    Map<String, Object> request = validRequest();
    request.put("expiry_month", lastMonth.getMonthValue());
    request.put("expiry_year", lastMonth.getYear());

    assertRejected(postPayment(request), "Expiry date must be in future");
  }

  @Test
  void whenAmountIsNotAnIntegerThenPaymentIsRejected() throws Exception {
    Map<String, Object> request = validRequest();
    request.put("amount", 10.50);

    assertRejected(postPayment(request), "Malformed request body");
  }

  @Test
  void whenBodyIsMalformedThenPaymentIsRejected() throws Exception {
    ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/payment")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"card_number\": "));

    assertRejected(result, "Malformed request body");
  }

  @Test
  void whenBankAuthorizesThenPaymentIsAuthorizedAndCanBeRetrieved() throws Exception {
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(true);
    when(bankClient.processPayment(any())).thenReturn(bankResponse);

    MvcResult result = postPayment(validRequest())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.card_number").doesNotExist())
        .andReturn();

    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"));
  }

  @Test
  void whenBankDeclinesThenPaymentIsDeclined() throws Exception {
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(false);
    when(bankClient.processPayment(any())).thenReturn(bankResponse);

    postPayment(validRequest())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.card_number").doesNotExist());
  }

  @Test
  void whenBankUnavailableThenBadGatewayIsReturned() throws Exception {
    when(bankClient.processPayment(any())).thenThrow(
        new BankUnavailableException("Bank request failed"));

    postPayment(validRequest())
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Bank unavailable, please try again later"));
  }

  private Map<String, Object> validRequest() {
    Map<String, Object> request = new HashMap<>();
    request.put("card_number", "2222405343248877");
    request.put("expiry_month", 4);
    request.put("expiry_year", YearMonth.now().getYear() + 1);
    request.put("currency", "GBP");
    request.put("amount", 100);
    request.put("cvv", "123");
    return request;
  }

  private ResultActions postPayment(Map<String, Object> request) throws Exception {
    return mvc.perform(MockMvcRequestBuilders.post("/payment")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }

  private void assertRejected(ResultActions result, String expectedError) throws Exception {
    result.andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0]").value(expectedError));
    verify(bankClient, never()).processPayment(any());
  }
}