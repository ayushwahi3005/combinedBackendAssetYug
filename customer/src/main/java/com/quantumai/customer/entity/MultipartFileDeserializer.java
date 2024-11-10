package com.quantumai.customer.entity;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;


public class MultipartFileDeserializer extends JsonDeserializer<MultipartFile> {



    @Override
    public MultipartFile deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = p.getCodec().readTree(p);
        String base64 = node.get("base64").asText();
        byte[] decoded = Base64.getDecoder().decode(base64);
        String name = node.get("name").asText();
        String originalFilename = node.get("originalFilename").asText();
        String contentType = node.get("contentType").asText();
        return new Base64MultipartFile(decoded, name, originalFilename, contentType);
    }
}
