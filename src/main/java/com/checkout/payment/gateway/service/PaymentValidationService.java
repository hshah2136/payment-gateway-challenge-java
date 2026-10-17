package com.checkout.payment.gateway.service;

import org.springframework.stereotype.Service;
import java.time.YearMonth;

@Service
public class PaymentValidationService {

  public boolean isExpiryDateValid(Integer month, Integer year) {
    if (month == null || year == null || month < 1 || month > 12) {
      return false;
    }

    YearMonth expiryDate = YearMonth.of(year, month);
    YearMonth currentDate = YearMonth.now();

    return !expiryDate.isBefore(currentDate);
  }
}
