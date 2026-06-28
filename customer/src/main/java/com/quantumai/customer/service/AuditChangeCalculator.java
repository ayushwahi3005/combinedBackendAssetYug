package com.quantumai.customer.service;

import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Utility to compute detailed field-level changes between two objects.
 * Used to populate audit log 'changes' field with {fieldName: {old: X, new: Y}} structure.
 */
@Slf4j
public class AuditChangeCalculator {

    private static final Set<String> IGNORED_FIELDS = Set.of(
            "id",           // MongoDB ObjectId
            "_id",
            "class",
            "createdAt",    // timestamps don't count as "changes"
            "updatedAt"
    );

    /**
     * Compares two objects and returns a map of fields that changed.
     * Format: { "fieldName": { "old": oldValue, "new": newValue }, ... }
     *
     * @param before The old/previous state
     * @param after  The new/updated state
     * @return Map of field changes, or empty map if no changes
     */
    public static Map<String, Object> computeChanges(Object before, Object after) {
        Map<String, Object> changes = new LinkedHashMap<>();

        if (before == null || after == null) {
            // Can't compare if one is null
            return changes;
        }

        if (before.getClass() != after.getClass()) {
            // Different types, can't compare
            return changes;
        }

        try {
            // Use reflection to get all fields from the object
            Field[] fields = before.getClass().getDeclaredFields();

            for (Field field : fields) {
                if (IGNORED_FIELDS.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                Object oldValue = field.get(before);
                Object newValue = field.get(after);

                // Check if values changed
                if (!Objects.equals(oldValue, newValue)) {
                    Map<String, Object> changeDetail = new LinkedHashMap<>();
                    changeDetail.put("old", oldValue != null ? oldValue.toString() : null);
                    changeDetail.put("new", newValue != null ? newValue.toString() : null);
                    changes.put(field.getName(), changeDetail);
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Could not compute changes via reflection: {}", e.getMessage());
        }

        return changes;
    }

    /**
     * Compare extra-field value maps (field name -> value) for asset/customer audits.
     */
    public static Map<String, Object> computeExtraFieldValueChanges(
            Map<String, String> before, Map<String, String> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        Set<String> allKeys = new LinkedHashSet<>();
        if (before != null) {
            allKeys.addAll(before.keySet());
        }
        if (after != null) {
            allKeys.addAll(after.keySet());
        }
        for (String key : allKeys) {
            String oldValue = before != null ? before.get(key) : null;
            String newValue = after != null ? after.get(key) : null;
            if (!Objects.equals(oldValue, newValue)) {
                Map<String, Object> changeDetail = new LinkedHashMap<>();
                changeDetail.put("old", oldValue != null ? oldValue : "");
                changeDetail.put("new", newValue != null ? newValue : "");
                changes.put(key, changeDetail);
            }
        }
        return changes;
    }
}
