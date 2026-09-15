package com.quantumai.customer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import com.quantumai.customer.entity.AssetUniqueFieldConfiguration;
import com.quantumai.customer.entity.InspectionStep;
import com.quantumai.customer.entity.InspectionStepValues;
import com.quantumai.customer.entity.InspectionTemplateResult;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to compute detailed field-level changes between two objects.
 * Used to populate audit log 'changes' field with {fieldName: {old: X, new: Y}} structure.
 */
@Slf4j
public class AuditChangeCalculator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> IGNORED_FIELDS = Set.of(
            "id",           // MongoDB ObjectId
            "_id",
            "class",
            "createdAt",    // timestamps don't count as "changes"
            "updatedAt",
            "createdBy",    // audit metadata fields
            "lastUpdatedBy"
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

                // Skip collection/map fields — they produce unreadable toString output
                if (oldValue instanceof java.util.Collection || oldValue instanceof java.util.Map
                        || newValue instanceof java.util.Collection || newValue instanceof java.util.Map) {
                    continue;
                }

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

    /**
     * Compare a single boolean flag (mandatory/show) between field-setting records.
     */
    public static Map<String, Object> computeMandatoryShowChanges(
            Object before, Object after, String fieldName) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (before == null || after == null) {
            return changes;
        }
        try {
            Field field = before.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            boolean oldValue = field.getBoolean(before);
            boolean newValue = field.getBoolean(after);
            if (oldValue != newValue) {
                Map<String, Object> changeDetail = new LinkedHashMap<>();
                changeDetail.put("old", String.valueOf(oldValue));
                changeDetail.put("new", String.valueOf(newValue));
                changes.put(fieldName, changeDetail);
            }
        } catch (ReflectiveOperationException e) {
            log.warn("Could not compare {} field: {}", fieldName, e.getMessage());
        }
        return changes;
    }

    public static Map<String, Object> computeUniqueFieldChanges(
            AssetUniqueFieldConfiguration before, AssetUniqueFieldConfiguration after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (before == null || after == null) {
            return changes;
        }
        if (!Objects.equals(before.getIsUnique(), after.getIsUnique())) {
            Map<String, Object> changeDetail = new LinkedHashMap<>();
            changeDetail.put("old", String.valueOf(before.getIsUnique()));
            changeDetail.put("new", String.valueOf(after.getIsUnique()));
            changes.put("isUnique", changeDetail);
        }
        if (!Objects.equals(before.getType(), after.getType())) {
            Map<String, Object> changeDetail = new LinkedHashMap<>();
            changeDetail.put("old", before.getType());
            changeDetail.put("new", after.getType());
            changes.put("type", changeDetail);
        }
        return changes;
    }

    public static Map<String, Object> fileUploadedChanges(String fileName, String fileId) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("fileName", changeDetail("", nullSafe(fileName)));
        if (fileId != null && !fileId.isBlank()) {
            changes.put("fileId", changeDetail("", fileId));
        }
        return changes;
    }

    public static Map<String, Object> fileDeletedChanges(String fileName, String fileId) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("fileName", changeDetail(nullSafe(fileName), ""));
        if (fileId != null && !fileId.isBlank()) {
            changes.put("fileId", changeDetail(fileId, ""));
        }
        return changes;
    }

    /**
     * Compare inspection template checkpoints (steps) for audit trail.
     */
    public static Map<String, Object> computeInspectionStepChanges(List<?> before, List<?> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        Map<String, String> beforeDefs = indexTemplateStepDefinitions(before);
        Map<String, String> afterDefs = indexTemplateStepDefinitions(after);

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(beforeDefs.keySet());
        allKeys.addAll(afterDefs.keySet());

        for (String key : allKeys) {
            String oldDef = beforeDefs.getOrDefault(key, "");
            String newDef = afterDefs.getOrDefault(key, "");
            if (!Objects.equals(oldDef, newDef)) {
                String label = "checkpoint: " + resolveTemplateStepLabel(key, oldDef, newDef);
                changes.put(label, changeDetail(oldDef, newDef));
            }
        }

        String oldSummary = summarizeInspectionSteps(before);
        String newSummary = summarizeInspectionSteps(after);
        if (!Objects.equals(oldSummary, newSummary)) {
            changes.put("checkpoints", changeDetail(oldSummary, newSummary));
        }
        return changes;
    }

    /**
     * Compare inspection instance checkpoint values for audit trail.
     */
    public static Map<String, Object> computeInspectionStepValueChanges(List<?> before, List<?> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        Map<String, Object> beforeMap = indexStepValues(before);
        Map<String, Object> afterMap = indexStepValues(after);
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(beforeMap.keySet());
        allKeys.addAll(afterMap.keySet());

        for (String key : allKeys) {
            Object oldStep = beforeMap.get(key);
            Object newStep = afterMap.get(key);
            String oldValue = readStepValue(oldStep);
            String newValue = readStepValue(newStep);
            if (!Objects.equals(oldValue, newValue)) {
                String label = resolveStepLabel(oldStep, newStep, key);
                changes.put(label, changeDetail(oldValue, newValue));
            }
        }
        return changes;
    }

    /**
     * Compare all checkpoint values on an inspection instance, including nested inspectionTemplates.
     */
    public static Map<String, Object> computeInspectionInstanceValueChanges(
            AssetCategoryInspectionInstance before, AssetCategoryInspectionInstance after) {
        return computeInspectionStepValueChanges(
                flattenInstanceStepValues(before),
                flattenInstanceStepValues(after));
    }

    private static Map<String, Object> changeDetail(String oldValue, String newValue) {
        Map<String, Object> changeDetail = new LinkedHashMap<>();
        changeDetail.put("old", oldValue != null ? oldValue : "");
        changeDetail.put("new", newValue != null ? newValue : "");
        return changeDetail;
    }

    private static String summarizeInspectionSteps(List<?> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        return steps.stream()
                .map(AuditChangeCalculator::summarizeSingleStep)
                .sorted()
                .collect(Collectors.joining(" | "));
    }

    private static String summarizeSingleStep(Object step) {
        if (step instanceof InspectionStep inspectionStep) {
            return formatStepSummary(
                    inspectionStep.getStepNumber(),
                    inspectionStep.getName(),
                    readTypeName(inspectionStep.getType()));
        }
        if (step instanceof Map<?, ?> map) {
            return formatStepSummary(
                    readLong(map.get("stepNumber")),
                    readString(map.get("name")),
                    readTypeName(map.get("type")));
        }
        return String.valueOf(step);
    }

    private static String formatStepSummary(long stepNumber, String name, String type) {
        return stepNumber + ": " + nullSafe(name) + " [" + nullSafe(type) + "]";
    }

    private static Map<String, String> indexTemplateStepDefinitions(List<?> steps) {
        Map<String, String> indexed = new LinkedHashMap<>();
        if (steps == null) {
            return indexed;
        }
        for (Object step : steps) {
            if (step == null) {
                continue;
            }
            String key = readTemplateStepKey(step);
            if (key != null && !key.isBlank()) {
                indexed.put(key, summarizeSingleStep(step));
            }
        }
        return indexed;
    }

    private static String readTemplateStepKey(Object step) {
        if (step instanceof InspectionStep inspectionStep) {
            if (inspectionStep.getId() != null && !inspectionStep.getId().isBlank()) {
                return inspectionStep.getId();
            }
            return inspectionStep.getStepNumber() + ":" + nullSafe(inspectionStep.getName());
        }
        if (step instanceof Map<?, ?> map) {
            String id = readString(map.get("id"));
            if (!id.isBlank()) {
                return id;
            }
            return readLong(map.get("stepNumber")) + ":" + readString(map.get("name"));
        }
        return String.valueOf(step);
    }

    private static String resolveTemplateStepLabel(String key, String oldDef, String newDef) {
        String fromDef = !newDef.isBlank() ? newDef : oldDef;
        int colon = fromDef.indexOf(':');
        if (colon > 0) {
            String namePart = fromDef.substring(colon + 1).trim();
            int bracket = namePart.indexOf('[');
            if (bracket > 0) {
                return namePart.substring(0, bracket).trim();
            }
            return namePart;
        }
        return key;
    }

    private static List<Object> flattenInstanceStepValues(AssetCategoryInspectionInstance instance) {
        if (instance == null) {
            return List.of();
        }
        List<Object> fromTemplates = new ArrayList<>();
        if (instance.getInspectionTemplates() != null) {
            for (Object template : instance.getInspectionTemplates()) {
                List<?> nested = readTemplateNestedStepValues(template);
                if (nested != null) {
                    fromTemplates.addAll(nested);
                }
            }
        }
        if (!fromTemplates.isEmpty()) {
            return fromTemplates;
        }
        if (instance.getStepValues() != null) {
            return new ArrayList<>(instance.getStepValues());
        }
        return List.of();
    }

    private static List<?> readTemplateNestedStepValues(Object template) {
        if (template instanceof InspectionTemplateResult result) {
            return result.getStepValues();
        }
        if (template instanceof Map<?, ?> map) {
            Object stepValues = map.get("stepValues");
            if (stepValues instanceof List<?> list) {
                return list;
            }
        }
        return null;
    }

    private static Map<String, Object> indexStepValues(List<?> steps) {
        Map<String, Object> indexed = new LinkedHashMap<>();
        if (steps == null) {
            return indexed;
        }
        for (Object step : steps) {
            if (step == null) {
                continue;
            }
            String key = readStepKey(step);
            if (key != null && !key.isBlank()) {
                indexed.put(key, step);
            }
        }
        return indexed;
    }

    private static String readStepKey(Object step) {
        if (step instanceof InspectionStepValues values) {
            String stepName = nullSafe(values.getName());
            if (!stepName.isBlank()) {
                return stepName;
            }
            if (values.getInspectionStepId() != null && !values.getInspectionStepId().isBlank()) {
                return values.getInspectionStepId();
            }
            return values.getId();
        }
        if (step instanceof Map<?, ?> map) {
            String stepName = readString(map.get("name"));
            if (!stepName.isBlank()) {
                return stepName;
            }
            String inspectionStepId = readString(map.get("inspectionStepId"));
            if (!inspectionStepId.isBlank()) {
                return inspectionStepId;
            }
            return readString(map.get("id"));
        }
        return null;
    }

    private static String readStepValue(Object step) {
        if (step instanceof InspectionStepValues values) {
            return formatAuditableValue(values.getValue());
        }
        if (step instanceof Map<?, ?> map) {
            return formatAuditableValue(map.get("value"));
        }
        return "";
    }

    private static String resolveStepLabel(Object oldStep, Object newStep, String fallbackKey) {
        String stepName = readStepName(newStep);
        if (stepName.isBlank()) {
            stepName = readStepName(oldStep);
        }
        if (!stepName.isBlank()) {
            return stepName;
        }
        return fallbackKey;
    }

    private static String readStepName(Object step) {
        if (step instanceof InspectionStepValues values) {
            return nullSafe(values.getName());
        }
        if (step instanceof Map<?, ?> map) {
            return readString(map.get("name"));
        }
        return "";
    }

    private static String readTypeName(Object type) {
        if (type == null) {
            return "";
        }
        if (type instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return type.toString();
    }

    private static String formatAuditableValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean boolValue) {
            return boolValue.toString();
        }
        return String.valueOf(value);
    }

    private static String readString(Object value) {
        return value != null ? value.toString() : "";
    }

    private static long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value != null ? value : List.of());
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
