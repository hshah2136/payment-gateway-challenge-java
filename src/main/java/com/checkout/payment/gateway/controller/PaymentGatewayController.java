package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentRejectedResponse;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found"),
      @ApiResponse(responseCode = "400", description = "Payment id is not a valid UUID",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Payment not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/payment/{id}")
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment Authorized or Declined by the bank"),
      @ApiResponse(responseCode = "400", description = "Payment Rejected, the bank was not called",
          content = @Content(schema = @Schema(implementation = PaymentRejectedResponse.class))),
      @ApiResponse(responseCode = "502", description = "Bank unavailable, payment not stored",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/payment")
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PostPaymentRequest request) {
    return new ResponseEntity<>(paymentGatewayService.processPayment(request), HttpStatus.OK);
  }
}
