package com.quantumai.customer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for validating company customer import data
 * Implements all import validation rules
 */
@Service
@Slf4j
public class CompanyCustomerImportValidationService {

    private static final int ZIP_CODE_MIN_LENGTH = 3;
    private static final int ZIP_CODE_MAX_LENGTH = 15;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\-\\+\\(\\)\\s]{10,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Rule 1: Check for duplicate email in system
     */
    public boolean isDuplicateEmailInSystem(String email, Long companyId, boolean emailExists) {
        return !email.isBlank() && emailExists;
    }

    /**
     * Rule 2: Validate status value against allowed system statuses
     */
    public boolean isValidStatus(String status) {
        if (status == null || status.isBlank()) {
            return true; // Allow blank if not mandatory
        }
        return status.equalsIgnoreCase("active") || status.equalsIgnoreCase("inactive");
    }

    /**
     * Rule 3: Validate custom field number type
     */
    public boolean isValidNumberField(String value, String fieldType) {
        if ("number".equalsIgnoreCase(fieldType)) {
            try {
                Integer.parseInt(value.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rule 4: Check if customer name is mandatory and provided
     */
    public boolean isValidCustomerName(String name, boolean isNameMandatory) {
        if (isNameMandatory) {
            return name != null && !name.isBlank();
        }
        return true;
    }

    /**
     * Rule 5: Check mandatory phone field
     */
    public boolean isValidMandatoryPhone(String phone, boolean isPhoneMandatory) {
        if (isPhoneMandatory) {
            return phone != null && !phone.isBlank();
        }
        return true;
    }

    /**
     * Rule 6: Validate phone number format
     */
    public boolean isValidPhoneFormat(String phone) {
        if (phone == null || phone.isBlank()) {
            return true; // Allow blank if not mandatory
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Rule 7: Validate state dropdown value
     */
    public boolean isValidStateDropdown(String state, List<String> validStates) {
        if (state == null || state.isBlank()) {
            return true; // Allow blank if not mandatory
        }
        return validStates.stream().anyMatch(s -> s.equalsIgnoreCase(state));
    }

    /**
     * Rule 8: Validate category dropdown value
     */
    public boolean isValidCategoryDropdown(String category, List<String> validCategories) {
        if (category == null || category.isBlank()) {
            return true; // Allow blank if not mandatory
        }
        return validCategories.stream().anyMatch(c -> c.equalsIgnoreCase(category));
    }

    /**
     * Rule 9: Validate ZIP code minimum length
     */
    public boolean isValidZipCodeMinLength(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            return true; // Allow blank if not mandatory
        }
        return zipCode.trim().length() >= ZIP_CODE_MIN_LENGTH;
    }

    /**
     * Rule 10: Validate ZIP code maximum length
     */
    public boolean isValidZipCodeMaxLength(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            return true; // Allow blank if not mandatory
        }
        return zipCode.trim().length() <= ZIP_CODE_MAX_LENGTH;
    }

    /**
     * Rule 11: Check if email is duplicated within import file
     */
    public boolean isDuplicateEmailInFile(String email, Set<String> emailsInFile) {
        if (email == null || email.isBlank()) {
            return false; // Blank emails are not considered duplicates
        }
        return emailsInFile.contains(email.toLowerCase());
    }

    /**
     * Rule 12: Check mandatory email field
     */
    public boolean isValidMandatoryEmail(String email, boolean isEmailMandatory) {
        if (isEmailMandatory) {
            return email != null && !email.isBlank();
        }
        return true; // Blank emails allowed if not mandatory
    }

    /**
     * Rule 13: Allow multiple blank emails
     * Multiple blank email entries should all be skipped if email is mandatory
     * Otherwise, allowed since blanks are not considered duplicates
     */
    public boolean canAllowBlankEmail(String email, boolean isEmailMandatory) {
        if (email == null || email.isBlank()) {
            return !isEmailMandatory;
        }
        return true;
    }

    /**
     * Validate email format
     */
    public boolean isValidEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Normalize status value (active/inactive)
     */
    public String normalizeStatus(String status) {
        if (status != null && status.equalsIgnoreCase("inactive")) {
            return "inActive";
        }
        return "active";
    }

    /**
     * Get validation error message for a specific rule
     */
    public String getValidationErrorMessage(String ruleName, Object... params) {
        switch (ruleName) {
            case "DUPLICATE_EMAIL":
                return "Email already exists in the system";
            case "INVALID_STATUS":
                return "Status value must be 'active' or 'inactive'";
            case "INVALID_NUMBER_FIELD":
                return "Custom field defined as Number but input is non-numeric";
            case "MISSING_CUSTOMER_NAME":
                return "Customer Name is mandatory and cannot be blank";
            case "MISSING_PHONE":
                return "Phone is mandatory and cannot be blank";
            case "INVALID_PHONE_FORMAT":
                return "Phone number format is invalid";
            case "INVALID_STATE":
                return "State value is not from the valid dropdown options";
            case "INVALID_CATEGORY":
                return "Category value is not from the valid dropdown options";
            case "ZIP_CODE_MIN_LENGTH":
                return "ZIP code must be at least " + ZIP_CODE_MIN_LENGTH + " characters";
            case "ZIP_CODE_MAX_LENGTH":
                return "ZIP code cannot exceed " + ZIP_CODE_MAX_LENGTH + " characters";
            case "DUPLICATE_EMAIL_IN_FILE":
                return "Email is duplicated within the import file";
            case "MISSING_EMAIL":
                return "Email is mandatory and cannot be blank";
            default:
                return "Validation error";
        }
    }
}

