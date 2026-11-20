package com.josef.marketbreath.service;

import com.josef.marketbreath.model.DailyClose;
import com.josef.marketbreath.model.PriceRow;
import com.josef.marketbreath.repo.DailyCloseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final List<String> SYMBOLS = List.of("SPY", "QQQ", "IWM");
    private static final List<Integer> PERIODS = List.of(10, 20, 50);
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    record TimeSeries(List<Bar> values, String message) {
        record Bar(String datetime, String close) {
        }
    }

    @Value("${twelvedata.api.key}")
    private String key;

    private final DailyCloseRepository repo;
    private final RestClient client = RestClient.create("https://api.twelvedata.com");

    public List<PriceRow> getRows() {
        Map<String, List<DailyClose>> bySymbol = repo.findAllByOrderByTradeDateAsc().stream()
                .collect(Collectors.groupingBy(DailyClose::getSymbol));

        // newest date first
        Map<LocalDate, Map<String, PriceRow.Signal>> byDate = new TreeMap<>(Comparator.reverseOrder());

        bySymbol.forEach((symbol, list) -> {
            for (int i = 0; i < list.size(); i++) {
                final int index = i;
                BigDecimal close = list.get(index).getClosePrice();

                List<PriceRow.Average> averages = PERIODS.stream()
                        .map(days -> {
                            BigDecimal ma = average(list, index, days);
                            return new PriceRow.Average(days, ma, ma == null ? null : close.compareTo(ma) > 0);
                        })
                        .toList();

                byDate.computeIfAbsent(list.get(index).getTradeDate(), d -> new HashMap<>())
                        .put(symbol, new PriceRow.Signal(close, averages));
            }
        });

        return byDate.entrySet().stream()
                .map(e -> new PriceRow(e.getKey(), e.getValue()))
                .toList();
    }

    // fetch newest days, but skip the request if we already have the last closed
    // day
    public void update() {
        Optional<DailyClose> latest = repo.findTopByOrderByTradeDateDesc();
        if (latest.isPresent() && !latest.get().getTradeDate().isBefore(lastClosedDay())) {
            return;
        }
        fetchAndSave(null);
    }

    // fetch days ending the day before our oldest stored date
    public void loadOlder() {
        LocalDate endDate = repo.findTopByOrderByTradeDateAsc()
                .map(c -> c.getTradeDate().minusDays(1))
                .orElse(null);
        fetchAndSave(endDate);
    }

    private void fetchAndSave(LocalDate endDate) {
        LocalDate lastClosed = lastClosedDay();

        for (String symbol : SYMBOLS) {
            TimeSeries series = fetch(symbol, endDate);
            if (series.values() == null) {
                throw new IllegalStateException("Twelve Data: " + series.message());
            }
            for (TimeSeries.Bar bar : series.values()) {
                LocalDate date = LocalDate.parse(bar.datetime());
                if (date.isAfter(lastClosed) || repo.existsBySymbolAndTradeDate(symbol, date)) {
                    continue;
                }
                BigDecimal close = new BigDecimal(bar.close()).setScale(2, RoundingMode.HALF_UP);
                repo.save(new DailyClose(date, symbol, close));
            }
        }
    }

    private TimeSeries fetch(String symbol, LocalDate endDate) {
        return client.get()
                .uri(b -> {
                    b.path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "1day")
                            .queryParam("outputsize", 30)
                            .queryParam("apikey", key);
                    if (endDate != null) {
                        b.queryParam("end_date", endDate);
                    }
                    return b.build();
                })
                .retrieve()
                .body(TimeSeries.class);
    }

    // average of the `days` closes ending at index `end`, or null if there aren't
    // enough yet
    private BigDecimal average(List<DailyClose> list, int end, int days) {
        if (end + 1 < days)
            return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = end - days + 1; i <= end; i++) {
            sum = sum.add(list.get(i).getClosePrice());
        }
        return sum.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
    }

    // most recent weekday whose market close has passed (ignores holidays)
    private LocalDate lastClosedDay() {
        ZonedDateTime ny = ZonedDateTime.now(NEW_YORK);
        LocalDate day = ny.toLocalDate();
        if (ny.toLocalTime().isBefore(LocalTime.of(16, 30))) {
            day = day.minusDays(1);
        }
        while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.minusDays(1);
        }
        return day;
    }
}