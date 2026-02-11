package com.breathego.repository;

import com.breathego.domain.DailyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyMetricsRepository extends JpaRepository<DailyMetrics, Long> {

    @Query("SELECT dm FROM DailyMetrics dm WHERE dm.location.id = :locationId AND dm.date >= :startDate ORDER BY dm.date ASC")
    List<DailyMetrics> findByLocationIdAndDateAfter(@Param("locationId") Long locationId, @Param("startDate") LocalDate startDate);

    @Query("SELECT dm FROM DailyMetrics dm WHERE dm.location.id = :locationId AND dm.date BETWEEN :startDate AND :endDate ORDER BY dm.date ASC")
    List<DailyMetrics> findByLocationIdAndDateBetween(
            @Param("locationId") Long locationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
