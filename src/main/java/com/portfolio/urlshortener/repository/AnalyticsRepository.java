package com.portfolio.urlshortener.repository;

import com.portfolio.urlshortener.entity.UrlAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<UrlAnalyticsEntity, Long> {
    List<UrlAnalyticsEntity> findByUrlIdAndClickTimeAfter(Long urlId, ZonedDateTime after);
    long countByUrlId(Long urlId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UrlAnalyticsEntity a WHERE a.urlId = :urlId")
    void deleteByUrlId(@Param("urlId") Long urlId);
}
