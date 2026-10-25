package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankPaymentResponse {

  // Boolean rather than boolean so a missing field is null, not silently false (Declined)
  private Boolean authorized;
  @JsonProperty("authorization_code")
  private String authorizationCode;

  public Boolean getAuthorized() {
    return authorized;
  }

  public void setAuthorized(Boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }

  @Override
  public String toString() {
    return "BankPaymentResponse{" +
        "authorized=" + authorized +
        ", authorizationCode='" + authorizationCode + '\'' +
        '}';
  }
}