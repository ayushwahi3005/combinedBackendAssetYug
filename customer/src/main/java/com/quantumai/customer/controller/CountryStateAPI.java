package com.quantumai.customer.controller;

import com.quantumai.customer.service.CountryStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/country")
public class CountryStateAPI {
    private final CountryStateService countryStateService;

    public CountryStateAPI(CountryStateService countryStateService) {
        this.countryStateService = countryStateService;
    }

    @GetMapping("/states/{country}")
    public ResponseEntity<List<String>> getStates(@PathVariable String country) {
        List<String> states = countryStateService.getStates(country);

        if (states == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(states);
    }
}
