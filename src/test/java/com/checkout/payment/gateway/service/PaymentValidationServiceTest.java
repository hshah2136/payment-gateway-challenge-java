package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class PaymentValidationServiceTest {

  private final PaymentValidationService validationService = new PaymentValidationService();

  @Test
  void whenExpiryIsCurrentMonthThenItIsValid() {
    YearMonth now = YearMonth.now();

    assertTrue(validationService.isExpiryDateValid(now.getMonthValue(), now.getYear()));
  }

  @Test
  void whenExpiryIsNextMonthThenItIsValid() {
    YearMonth nextMonth = YearMonth.now().plusMonths(1);

    assertTrue(validationService.isExpiryDateValid(nextMonth.getMonthValue(), nextMonth.getYear()));
  }

  @Test
  void whenExpiryIsInAFutureYearThenItIsValid() {
    YearMonth future = YearMonth.now().plusYears(3);

    assertTrue(validationService.isExpiryDateValid(future.getMonthValue(), future.getYear()));
  }

  @Test
  void whenExpiryWasLastMonthThenItIsInvalid() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);

    assertFalse(validationService.isExpiryDateValid(lastMonth.getMonthValue(), lastMonth.getYear()));
  }

  @Test
  void whenExpiryIsInAPastYearThenItIsInvalid() {
    YearMonth past = YearMonth.now().minusYears(1);

    assertFalse(validationService.isExpiryDateValid(past.getMonthValue(), past.getYear()));
  }

  @Test
  void whenMonthIsNullThenItIsInvalid() {
    assertFalse(validationService.isExpiryDateValid(null, YearMonth.now().getYear() + 1));
  }

  @Test
  void whenYearIsNullThenItIsInvalid() {
    assertFalse(validationService.isExpiryDateValid(6, null));
  }

  @Test
  void whenMonthIsZeroThenItIsInvalid() {
    assertFalse(validationService.isExpiryDateValid(0, YearMonth.now().getYear() + 1));
  }

  @Test
  void whenMonthIsThirteenThenItIsInvalid() {
    assertFalse(validationService.isExpiryDateValid(13, YearMonth.now().getYear() + 1));
  }
}