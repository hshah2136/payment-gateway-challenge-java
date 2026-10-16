package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankPaymentRequest {

  @JsonProperty("card_number")
  private final String cardNumber;
  @JsonProperty("expiry_date")
  private final String expiryDate;
  private final String currency;
  private final int amount;
  private final String cvv;

  public BankPaymentRequest(String cardNumber, int expiryMonth, int expiryYear, String currency,
      int amount, String cvv) {
    this.cardNumber = cardNumber;
    this.expiryDate = String.format("%02d/%d", expiryMonth, expiryYear);
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public String getCurrency() {
    return currency;
  }

  public int getAmount() {
    return amount;
  }

  public String getCvv() {
    return cvv;
  }

  // Card number and CVV are deliberately excluded so they never end up in logs
  @Override
  public String toString() {
    return "BankPaymentRequest{" +
        "expiryDate='" + expiryDate + '\'' +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
