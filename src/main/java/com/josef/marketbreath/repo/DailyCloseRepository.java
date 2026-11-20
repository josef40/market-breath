package com.josef.marketbreath.repo;

import com.josef.marketbreath.model.DailyClose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCloseRepository extends JpaRepository<DailyClose, Long> {

    boolean existsBySymbolAndTradeDate(String symbol, LocalDate tradeDate);

    List<DailyClose> findAllByOrderByTradeDateAsc();

    Optional<DailyClose> findTopByOrderByTradeDateDesc();

    Optional<DailyClose> findTopByOrderByTradeDateAsc();
}