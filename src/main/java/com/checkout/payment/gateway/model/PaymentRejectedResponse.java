package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.List;

public class PaymentRejectedResponse {
  private final PaymentStatus status = PaymentStatus.REJECTED;
  private final List<String> errors;

  public PaymentRejectedResponse(List<String> errors) {
    this.errors = errors;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public List<String> getErrors() {
    return errors;
  }
}
