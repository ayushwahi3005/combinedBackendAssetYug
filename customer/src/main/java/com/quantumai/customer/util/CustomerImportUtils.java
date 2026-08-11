package com.quantumai.customer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public final class CustomerImportUtils {

  private static final List<DateTimeFormatter> DATE_FORMATTERS =
      Arrays.asList(
          DateTimeFormatter.ofPattern("M/d/yyyy"),
          DateTimeFormatter.ofPattern("MM/dd/yyyy"),
          DateTimeFormatter.ofPattern("d/M/yyyy"),
          DateTimeFormatter.ofPattern("dd/MM/yyyy"),
          DateTimeFormatter.ofPattern("dd-MM-yyyy"),
          DateTimeFormatter.ofPattern("yyyy-MM-dd"));

  private CustomerImportUtils() {}

  public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  public static String normalizeMappedField(String mappedField) {
    if (mappedField == null) {
      return null;
    }
    return mappedField.trim().toLowerCase().replace("_", " ");
  }

  public static boolean isZipField(String mappedField) {
    String normalized = normalizeMappedField(mappedField);
    return "zip code".equals(normalized) || "zipcode".equals(normalized.replace(" ", ""));
  }

  public static boolean isCountryField(String mappedField) {
    return "country".equals(normalizeMappedField(mappedField));
  }

  public static boolean isValidOptionalZip(String rawZip, boolean mandatory) {
    if (isBlank(rawZip)) {
      return !mandatory;
    }
    String zipValue = normalizeZipValue(rawZip.trim());
    return zipValue.length() >= 3 && zipValue.length() <= 15;
  }

  public static String normalizeZipValue(String rawZip) {
    if (isBlank(rawZip)) {
      return rawZip;
    }
    String zipValue = rawZip.trim();
    try {
      if (zipValue.contains("E") || zipValue.contains("e")) {
        java.math.BigDecimal bd = new java.math.BigDecimal(zipValue);
        zipValue = bd.toPlainString();
      }
    } catch (NumberFormatException ignored) {
      // keep raw value
    }
    if (zipValue.matches("\\d+\\.0+")) {
      zipValue = zipValue.replaceAll("\\.0+$", "");
    }
    return zipValue;
  }

  public static String normalizeCountryName(String country) {
    if (isBlank(country)) {
      return country;
    }
    String trimmed = country.trim();
    if (trimmed.equalsIgnoreCase("United States")
            || trimmed.equalsIgnoreCase("USA")
            || trimmed.equalsIgnoreCase("US")) {
      return "United States of America";
    }
    return trimmed;
  }

  public static boolean isValidCountry(String country, java.util.Collection<String> countryList) {
    if (isBlank(country)) {
      return false;
    }
    String normalized = normalizeCountryName(country);
    return countryList.stream().anyMatch(valid -> valid.equalsIgnoreCase(normalized));
  }

  public static String parseOptionalNumber(String value, boolean mandatory) {
    if (isBlank(value)) {
      return mandatory ? null : "";
    }
    Integer.parseInt(value.trim());
    return value.trim();
  }

  public static String parseImportDate(String value) {
    if (isBlank(value)) {
      return "";
    }
    String trimmed = value.trim();
    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
      try {
        LocalDate date = LocalDate.parse(trimmed, formatter);
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } catch (DateTimeParseException ignored) {
        // try next format
      }
    }
    throw new IllegalArgumentException("Invalid date format: " + value);
  }

  public static boolean isStatusMapped(java.util.Collection<String> mappedValues) {
    return mappedValues.stream()
        .anyMatch(value -> "status".equals(normalizeMappedField(value)));
  }

  public static String getImportCellValue(String[] row, int index) {
    if (row == null || index < 0 || index >= row.length || row[index] == null) {
      return "";
    }
    return row[index].trim();
  }

  public static boolean isNameMapped(java.util.Collection<String> mappedValues) {
    return mappedValues.stream()
        .anyMatch(value -> "name".equals(normalizeMappedField(value)));
  }

  public static void appendImportError(StringBuilder errorDesc, String message) {
    if (errorDesc.length() > 0) {
      errorDesc.append(", ");
    }
    errorDesc.append(message);
  }
}
