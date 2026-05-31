package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.quantumai.customer.service.CountryStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/country")
@Tag(name = "CountryState", description = "CountryState Management API")
public class CountryStateAPI {
    private final CountryStateService countryStateService;

    public CountryStateAPI(CountryStateService countryStateService) {
        this.countryStateService = countryStateService;
    }

    @Operation(summary = "Get States", description = "Endpoint to get states")
    @GetMapping("/states/{country}")
    public ResponseEntity<List<String>> getStates(@PathVariable String country) {
        List<String> states = countryStateService.getStates(country);

        if (states == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(states);
    }
}
