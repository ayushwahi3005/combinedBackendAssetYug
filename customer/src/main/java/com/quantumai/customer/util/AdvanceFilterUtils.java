package com.quantumai.customer.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.dto.AssetAdvancedFilterDTO;
import com.quantumai.customer.dto.AssetWithCustomFieldsDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdvanceFilterUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Set<String> FILTER_METADATA_KEYS =
      Set.of("companyId", "search", "searchData");

  private static final Set<String> CUSTOM_FIELD_SEARCH_KEYS =
      Set.of("search", "searchData", "searchTerm");

  private static final Set<String> CUSTOM_FIELD_CHECK_IN_OUT_KEYS =
      Set.of("checkedinout", "checkinoutstatus", "checkedinoutstatus", "checkedin", "checkedout");

  private AdvanceFilterUtils() {}

  public static boolean isCustomFieldCheckInOutKey(String key) {
    return key != null && CUSTOM_FIELD_CHECK_IN_OUT_KEYS.contains(key.toLowerCase());
  }

  public static boolean isCustomFieldSearchKey(String key) {
    return key != null && CUSTOM_FIELD_SEARCH_KEYS.contains(key.toLowerCase());
  }

  /**
   * Pull global search out of customFields (UI sends it there) and remove metadata keys
   * so they are not treated as asset extra-field filters.
   */
  public static void normalizeAssetAdvancedFilter(AssetAdvancedFilterDTO filter, String queryParamSearch) {
    if (filter == null) {
      return;
    }

    if (filter.getCustomFields() != null && !filter.getCustomFields().isEmpty()) {
      Map<String, String> customFields = filter.getCustomFields();
      for (Map.Entry<String, String> entry : new ArrayList<>(customFields.entrySet())) {
        if (isCustomFieldSearchKey(entry.getKey())) {
          String fromCustomField = normalizeSearch(entry.getValue());
          if (!fromCustomField.isEmpty() && normalizeSearch(filter.getSearch()).isEmpty()) {
            filter.setSearch(fromCustomField);
          }
          customFields.remove(entry.getKey());
        } else if (isCustomFieldCheckInOutKey(entry.getKey())) {
          if (resolveCheckedInOutFilter(filter.getCheckedInOut()) == null) {
            String resolved = resolveCheckedInOutFilter(entry.getValue());
            if (resolved == null && "true".equalsIgnoreCase(entry.getValue())) {
              resolved = entry.getKey().toLowerCase().contains("out")
                  ? "Checked Out" : "Checked In";
            }
            if (resolved != null) {
              filter.setCheckedInOut(resolved);
            }
          }
          customFields.remove(entry.getKey());
        }
      }
      customFields.entrySet().removeIf(
          entry -> entry.getValue() == null || entry.getValue().trim().isEmpty());
    }

    String resolvedSearch = resolveAssetFilterSearch(filter, queryParamSearch);
    if (!resolvedSearch.isEmpty()) {
      filter.setSearch(resolvedSearch);
    }

    String resolvedCheckInOut = resolveCheckedInOutFilter(filter.getCheckedInOut());
    if (resolvedCheckInOut != null) {
      filter.setCheckedInOut(resolvedCheckInOut);
    }
  }

  public static boolean isFilterMetadataKey(String key) {
    return key != null && FILTER_METADATA_KEYS.contains(key);
  }

  public static String extractSearchFromFilterMap(Map<?, ?> filterMap) {
    if (filterMap == null) {
      return "";
    }
    Object search = filterMap.get("search");
    if (search == null) {
      search = filterMap.get("searchData");
    }
    return normalizeSearch(search != null ? search.toString() : null);
  }

  public static String resolveSearchTerm(String queryParamSearch, Map<?, ?> filterMap) {
    String bodySearch = extractSearchFromFilterMap(filterMap);
    if (!bodySearch.isEmpty()) {
      return bodySearch;
    }
    return normalizeSearch(queryParamSearch);
  }

  public static String normalizeSearch(String search) {
    if (search == null || search.isEmpty() || "null".equalsIgnoreCase(search)) {
      return "";
    }
    return search.trim();
  }

  /**
   * Normalizes check-in/out filter values to {@code Checked In} or {@code Checked Out}.
   */
  public static String resolveCheckedInOutFilter(String raw) {
    if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw)) {
      return null;
    }
    String normalized = raw.trim().toLowerCase().replace("_", " ").replace("-", " ");
    if (normalized.equals("checked out")
        || normalized.equals("checkedout")
        || normalized.equals("out")
        || normalized.equals("false")) {
      return "Checked Out";
    }
    if (normalized.equals("checked in")
        || normalized.equals("checkedin")
        || normalized.equals("in")
        || normalized.equals("true")) {
      return "Checked In";
    }
    if (normalized.contains("checked out") || normalized.endsWith(" out")) {
      return "Checked Out";
    }
    if (normalized.contains("checked in") || normalized.endsWith(" in")) {
      return "Checked In";
    }
    return null;
  }

  public static String resolveAssetFilterSearch(AssetAdvancedFilterDTO filter, String queryParamSearch) {
    String fromBody = normalizeSearch(filter != null ? filter.getSearch() : null);
    if (!fromBody.isEmpty()) {
      return fromBody;
    }
    String fromQuery = normalizeSearch(queryParamSearch);
    if (!fromQuery.isEmpty()) {
      return fromQuery;
    }
    if (filter != null && filter.getCustomFields() != null) {
      for (String key : CUSTOM_FIELD_SEARCH_KEYS) {
        String fromCustom = normalizeSearch(filter.getCustomFields().get(key));
        if (!fromCustom.isEmpty()) {
          return fromCustom;
        }
      }
    }
    return detectImplicitSearchFromDuplicateFilters(filter);
  }

  /**
   * Some clients copy the global search text into every text filter field. When all of those
   * fields share the same value, treat it as a global search instead of AND-ing them in Mongo.
   */
  public static String detectImplicitSearchFromDuplicateFilters(AssetAdvancedFilterDTO filter) {
    if (filter == null) {
      return "";
    }
    List<String> textValues = new ArrayList<>();
    addIfPresent(textValues, filter.getName());
    addIfPresent(textValues, filter.getCustomer());
    addIfPresent(textValues, filter.getSerialNumber());
    addIfPresent(textValues, filter.getCategory());
    addIfPresent(textValues, filter.getEmail());
    addIfPresent(textValues, filter.getAssetId());
    if (textValues.isEmpty()) {
      return "";
    }
    String first = textValues.get(0);
    boolean allSame = textValues.stream().allMatch(value -> value.equalsIgnoreCase(first));
    return allSame ? first : "";
  }

  private static void addIfPresent(List<String> values, String value) {
    String normalized = normalizeSearch(value);
    if (!normalized.isEmpty()) {
      values.add(normalized);
    }
  }

  public static boolean matchesSearch(String jsonData, String searchTerm) {
    String normalized = normalizeSearch(searchTerm);
    if (normalized.isEmpty()) {
      return true;
    }
    try {
      Map<String, String> map =
          OBJECT_MAPPER.readValue(jsonData, new TypeReference<Map<String, String>>() {});
      String lowerSearch = normalized.toLowerCase();
      return map.entrySet().stream()
          .filter(entry -> !"id".equals(entry.getKey()) && !"image".equals(entry.getKey()))
          .anyMatch(
              entry ->
                  entry.getValue() != null
                      && entry.getValue().toLowerCase().contains(lowerSearch));
    } catch (Exception e) {
      return jsonData.toLowerCase().contains(normalized.toLowerCase());
    }
  }

  public static boolean matchesAssetSearch(AssetWithCustomFieldsDTO asset, String searchTerm) {
    String normalized = normalizeSearch(searchTerm);
    if (normalized.isEmpty()) {
      return true;
    }
    String lowerSearch = normalized.toLowerCase();
    if (containsIgnoreCase(asset.getName(), lowerSearch)
        || containsIgnoreCase(asset.getSerialNumber(), lowerSearch)
        || containsIgnoreCase(asset.getCategory(), lowerSearch)
        || containsIgnoreCase(asset.getCustomer(), lowerSearch)
        || containsIgnoreCase(asset.getCustomerId(), lowerSearch)
        || containsIgnoreCase(asset.getLocation(), lowerSearch)
        || containsIgnoreCase(asset.getLocationName(), lowerSearch)
        || containsIgnoreCase(asset.getStatus(), lowerSearch)
        || containsIgnoreCase(asset.getEmail(), lowerSearch)
        || containsIgnoreCase(asset.getCheckedInOutStatus(), lowerSearch)) {
      return true;
    }
    if (asset.getAssetId() != null
        && String.valueOf(asset.getAssetId()).toLowerCase().contains(lowerSearch)) {
      return true;
    }
    if (asset.getCustomFields() != null) {
      for (String value : asset.getCustomFields().values()) {
        if (containsIgnoreCase(value, lowerSearch)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean containsIgnoreCase(String value, String lowerSearch) {
    return value != null && value.toLowerCase().contains(lowerSearch);
  }
}
