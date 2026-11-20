package com.josef.marketbreath.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "trade_date", "symbol" }))
@Getter
@NoArgsConstructor
public class DailyClose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate tradeDate;
    private String symbol;

    @Column(precision = 12, scale = 2)
    private BigDecimal closePrice;

    public DailyClose(LocalDate tradeDate, String symbol, BigDecimal closePrice) {
        this.tradeDate = tradeDate;
        this.symbol = symbol;
        this.closePrice = closePrice;
    }
}