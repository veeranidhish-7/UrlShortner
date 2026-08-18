package com.portfolio.urlshortener.repository;

import com.portfolio.urlshortener.entity.UrlAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<UrlAnalyticsEntity, Long> {
    List<UrlAnalyticsEntity> findByUrlIdAndClickTimeAfter(Long urlId, ZonedDateTime after);
    long countByUrlId(Long urlId);
}
