package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.quantumai.customer.service.CountryStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/country")
@Tag(name = "CountryState", description = "CountryState Management API")
@Slf4j
public class CountryStateAPI {
    private final CountryStateService countryStateService;

    public CountryStateAPI(CountryStateService countryStateService) {
        this.countryStateService = countryStateService;
    }

    @Operation(summary = "Get States", description = "Endpoint to get states")
    @GetMapping("/states/{country}")
    public ResponseEntity<List<String>> getStates(@PathVariable String country) {
        log.info("Fetching states for country: {}", country);
        List<String> states = countryStateService.getStates(country);
        log.info("Fetching states: {}", states);

        if (states == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(states);
    }
}
