package com.josef.marketbreath.controller;

import com.josef.marketbreath.model.PriceRow;
import com.josef.marketbreath.service.MarketService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
public class MarketController {
    private final MarketService service;

    @GetMapping
    public List<PriceRow> prices() {
        return service.getRows();
    }

    @PostMapping("/update")
    public List<PriceRow> update() {
        service.update();
        return service.getRows();
    }

    @PostMapping("/older")
    public List<PriceRow> older() {
        service.loadOlder();
        return service.getRows();
    }
}