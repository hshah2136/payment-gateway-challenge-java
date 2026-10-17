package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentRejectedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.List;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
    LOG.warn(ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse("Payment not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PaymentRejectedException.class)
  public ResponseEntity<PaymentRejectedResponse> handlePaymentRejected(PaymentRejectedException ex) {
    return rejected(List.of(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<PaymentRejectedResponse> handleInvalidRequest(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getAllErrors().stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
    return rejected(errors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<PaymentRejectedResponse> handleUnreadableRequest(
      HttpMessageNotReadableException ex) {
    return rejected(List.of("Malformed request body"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPaymentId(MethodArgumentTypeMismatchException ex) {
    LOG.warn("Invalid value for parameter '{}'", ex.getName());
    return new ResponseEntity<>(new ErrorResponse("Invalid payment id"), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    LOG.error("Unexpected error", ex);
    return new ResponseEntity<>(new ErrorResponse("Internal server error"),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<PaymentRejectedResponse> rejected(List<String> errors) {
    LOG.warn("Payment rejected: {}", errors);
    return new ResponseEntity<>(new PaymentRejectedResponse(errors), HttpStatus.BAD_REQUEST);
  }
}
