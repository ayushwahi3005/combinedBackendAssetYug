package com.quantumai.customer.entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.quantumai.customer.dto.CustomRoleDTO;
import java.io.IOException;

/** Accepts role as either a string (legacy) or a CustomRole object from the frontend. */
public class MailRoleDeserializer extends JsonDeserializer<CustomRoleDTO> {

  @Override
  public CustomRoleDTO deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      CustomRoleDTO role = new CustomRoleDTO();
      role.setName(node.asText());
      return role;
    }
    if (node.isObject()) {
      return context.readTreeAsValue(node, CustomRoleDTO.class);
    }
    return null;
  }
}
