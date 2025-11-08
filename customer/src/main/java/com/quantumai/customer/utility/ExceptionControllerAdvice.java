package com.quantumai.customer.utility;

import com.quantumai.customer.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
    errorInfo.setErrorMessage("No active subscription. Please subscribe to a Plan");
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

  @ExceptionHandler(UserEmailAlreadyVerifiedException.class)
  public ResponseEntity<ErrorInfo> UserEmailAlreadyVerifiedException(UserEmailAlreadyVerifiedException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("This email is already verified and active");
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
  public ResponseEntity<ErrorInfo> ExtraFieldAlreadyPresentException(
      ExtraFieldAlreadyPresentException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Extra Field Already Present");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(PlanDowngradeException.class)
  public ResponseEntity<ErrorInfo> PlanDowngradeException(PlanDowngradeException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage(
        "Cannot Start Upcoming Subscription as Upcoming Subscription Person is less than Current Subscription");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(PlanPersonException.class)
  public ResponseEntity<ErrorInfo> PlanPersonException(PlanPersonException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Subscription Combination is Already Active");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RoleDeletionException.class)
  public ResponseEntity<ErrorInfo> RoleDeletionException(RoleDeletionException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Some User is Already Using this Role");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErrorInfo> EmailAlreadyExistsException(EmailAlreadyExistsException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Customer with this Email Already Exists");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NameColumnMissingException.class)
  public ResponseEntity<ErrorInfo> NameColumnMissingException(NameColumnMissingException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Mandatory Column Name Is Missing in Mapping");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(SamePasswordException.class)
  public ResponseEntity<ErrorInfo> SamePasswordException(SamePasswordException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("New Password Cannot be same as old password");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(LocationAlreadyPresentException.class)
  public ResponseEntity<ErrorInfo> LocationAlreadyPresentException(LocationAlreadyPresentException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Location With Given Name Already Present");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UserCannotActivateException.class)
  public ResponseEntity<ErrorInfo> UserCannotActivateException(UserCannotActivateException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("User Cannot be Activated. Either Upgrade Subscription for more person or disable any other User");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UserAccessException.class)
  public ResponseEntity<ErrorInfo> UserCannotActivateException(UserAccessException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("You Dont Have Access For this Request");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorInfo> MaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Upload File Size Exceeds");
    errorInfo.setErrorCode(HttpStatus.PAYLOAD_TOO_LARGE.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.PAYLOAD_TOO_LARGE);
  }

}
