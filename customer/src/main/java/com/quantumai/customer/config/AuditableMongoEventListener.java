package com.quantumai.customer.config;

import com.quantumai.customer.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

@Component
@RequiredArgsConstructor
public class AuditableMongoEventListener extends AbstractMongoEventListener<Object> {

  private final CurrentUserService currentUserService;

  @Override
  public void onBeforeConvert(BeforeConvertEvent<Object> event) {
    Object source = event.getSource();
    if (source == null) {
      return;
    }

    Field createdByField = ReflectionUtils.findField(source.getClass(), "createdBy");
    Field lastUpdatedByField = ReflectionUtils.findField(source.getClass(), "lastUpdatedBy");
    if (createdByField == null && lastUpdatedByField == null) {
      return;
    }

    String displayName = currentUserService.getCurrentUserDisplayName();
    if (displayName == null || displayName.isBlank()) {
      return;
    }

    if (createdByField != null) {
      ReflectionUtils.makeAccessible(createdByField);
      String existing = (String) ReflectionUtils.getField(createdByField, source);
      if (existing == null || existing.isBlank()) {
        ReflectionUtils.setField(createdByField, source, displayName);
      }
    }

    if (lastUpdatedByField != null) {
      ReflectionUtils.makeAccessible(lastUpdatedByField);
      ReflectionUtils.setField(lastUpdatedByField, source, displayName);
    }
  }
}
