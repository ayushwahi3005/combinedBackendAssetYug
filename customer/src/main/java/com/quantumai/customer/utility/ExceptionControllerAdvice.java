package com.quantumai.customer.utility;

import com.quantumai.customer.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionControllerAdvice {
  @ExceptionHandler(UserAlreadyPresentException.class)
  public ResponseEntity<ErrorInfo> UserAlreadyPresentException(
      UserAlreadyPresentException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Email Already Registered");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoSubscriptionError.class)
  public ResponseEntity<ErrorInfo> NoSubscriptionErrorException(NoSubscriptionError exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("No Subscription");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UserNotFound.class)
  public ResponseEntity<ErrorInfo> UserNotFound(UserNotFound exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("User Not Associated to any company");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(CategoryException.class)
  public ResponseEntity<ErrorInfo> UserNotFound(CategoryException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Category Already Present");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UserException.class)
  public ResponseEntity<ErrorInfo> UserNotFound(UserException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("User Already Invited");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(WrongAdminEmailException.class)
  public ResponseEntity<ErrorInfo> WrongAdminEmailException(WrongAdminEmailException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Wrong Admin Email");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoEmailFoundException.class)
  public ResponseEntity<ErrorInfo> NoEmailFoundException(NoEmailFoundException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("No Such Email Found");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(OTPException.class)
  public ResponseEntity<ErrorInfo> OTPException(OTPException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("OTP is wrong or expired");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(WrongCredentialException.class)
  public ResponseEntity<ErrorInfo> WrongCredentialException(WrongCredentialException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Wrong Credential");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }
  @ExceptionHandler(ExtraFieldAlreadyPresentException.class)
  public ResponseEntity<ErrorInfo> ExtraFieldAlreadyPresentException(ExtraFieldAlreadyPresentException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Extra Field Already Present");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(PlanDowngradeException.class)
  public ResponseEntity<ErrorInfo> PlanDowngradeException(PlanDowngradeException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Cannot downgrade from Annual to Monthly");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(PlanPersonException.class)
  public ResponseEntity<ErrorInfo> PlanPersonException(PlanPersonException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Person count cannot be less than or equal to the existing subscription");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }


}
