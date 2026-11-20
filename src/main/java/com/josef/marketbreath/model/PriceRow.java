package com.josef.marketbreath.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PriceRow(LocalDate date, Map<String, Signal> signals) {

    public record Signal(BigDecimal close, List<Average> averages) {
    }

    // value and above are null when there isn't enough history yet
    public record Average(int days, BigDecimal value, Boolean above) {
    }
}