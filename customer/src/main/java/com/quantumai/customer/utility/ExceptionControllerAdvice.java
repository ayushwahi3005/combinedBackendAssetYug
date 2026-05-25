package com.quantumai.customer.utility;

import com.quantumai.customer.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;


@RestControllerAdvice
public class ExceptionControllerAdvice {

  @Value("${max_import_rows_count}")
  private String max_import_rows_count;

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

  @ExceptionHandler(LocationDeletionException.class)
  public ResponseEntity<ErrorInfo> LocationDeletionException(LocationDeletionException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Location/Bin is assigned to assets, cannot delete. Remove it from all assets first.");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }


  @ExceptionHandler(ExtraFieldDeletionException.class)
  public ResponseEntity<ErrorInfo> ExtraFieldDeletionException(ExtraFieldDeletionException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage(
            "Custom field can't be deleted as data exists for " +
                    exception.getCount() +
                    " customers, please make the field Inactive"
    );
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }


  @ExceptionHandler(AssetExtraFieldDeletionException.class)
  public ResponseEntity<ErrorInfo> AssetExtraFieldDeletionException(AssetExtraFieldDeletionException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage(
            "Custom field can't be deleted as data exists for " +
                    exception.getCount() +
                    " assets, please make the field Inactive");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BinAlreadyPresentException.class)
  public ResponseEntity<ErrorInfo> BinAlreadyPresentException(BinAlreadyPresentException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Bin Number Already Present in this Location");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ImportFileRowException.class)
  public ResponseEntity<ErrorInfo> ImportFileRowException(ImportFileRowException exception) {
    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setErrorMessage("Upload Limit Exceeded: The system only allows a maximum of 1,000 records per upload. Please modify your file and re-upload");
    errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ImportInProgressException.class)
  public ResponseEntity<Map<String, String>> handleImportInProgress(ImportInProgressException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)  // 409
            .body(Map.of(
                    "error", "IMPORT_IN_PROGRESS",
                    "message", ex.getMessage()
            ));
  }

  @ExceptionHandler(AssetUniqueFieldViolationException.class)
  public ResponseEntity<Map<String, Object>> handleAssetUniqueFieldViolation(AssetUniqueFieldViolationException exception) {
    Map<String, Object> response = new java.util.HashMap<>();
    response.put("error", "UNIQUE_FIELD_VIOLATION");
    response.put("message", exception.getMessage());
    
    // Return all field names from conflicts map instead of single fieldName
    if (exception.getConflicts() != null && !exception.getConflicts().isEmpty()) {
      response.put("fieldNames", exception.getConflicts().keySet());
    } else {
      response.put("fieldName", exception.getFieldName());
    }
    
    response.put("conflicts", exception.getConflicts());

    if (exception.getValidationDetails() != null) {
      response.put("validationDetails", exception.getValidationDetails());
    }

    return ResponseEntity
            .status(HttpStatus.CONFLICT)  // 409
            .body(response);
  }

}
