package com.quantumai.customer.util;

public final class PhoneUtils {

  private PhoneUtils() {}

  /**
   * Cleans phone input without forcing a country code prefix.
   */
  public static String normalizeForStorage(String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      return phone;
    }
    return phone.trim();
  }

  /** Import validation: 3-15 chars, no letters. */
  public static boolean isValidImportPhone(String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      return true;
    }
    return phone.trim().matches("^[^a-zA-Z]{3,15}$");
  }

  /**
   * Formats a phone number into US style: (XXX) XXX-XXXX.
   * Strips any non-digit characters first, and drops a leading
   * country code "1" if present (i.e. 11 digits starting with 1).
   * If the result isn't exactly 10 digits, the original trimmed
   * input is returned unchanged (e.g. short/international numbers).
   */
  public static String formatToUSStyle(String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      return phone;
    }

    String trimmed = phone.trim();
    String digits = trimmed.replaceAll("[^0-9]", "");

    if (digits.length() == 11 && digits.startsWith("1")) {
      digits = digits.substring(1);
    }

    if (digits.length() == 10) {
      String area = digits.substring(0, 3);
      String prefix = digits.substring(3, 6);
      String line = digits.substring(6, 10);
      return String.format("(%s) %s-%s", area, prefix, line);
    }

    // Not a standard 10-digit US number — leave as-is rather than
    // silently mangling international/short entries.
    return trimmed;
  }
}