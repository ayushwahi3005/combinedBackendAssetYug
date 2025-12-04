package com.quantumai.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class CountryStateService {

    private Map<String, List<String>> data;

    @PostConstruct
    public void loadJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/Country-States.json");

            data = mapper.readValue(
                    is,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, List.class)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load country-state JSON", e);
        }
    }

    public List<String> getStates(String country) {
        return data.get(country);   // key must match exactly
    }
}
