package com.portfolio.urlshortener.service;

import com.portfolio.urlshortener.entity.UrlAnalyticsEntity;
import com.portfolio.urlshortener.entity.UrlEntity;
import com.portfolio.urlshortener.event.UrlClickEvent;
import com.portfolio.urlshortener.repository.AnalyticsRepository;
import com.portfolio.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final UrlRepository urlRepository;

    @Transactional
    public void processClickEvent(UrlClickEvent event) {
        // Increment the total click count for the URL
        urlRepository.findById(event.getUrlId()).ifPresent(url -> {
            url.setClickCount(url.getClickCount() + 1);
            urlRepository.save(url);
        });

        // Save detailed telemetry
        UrlAnalyticsEntity analytics = new UrlAnalyticsEntity();
        analytics.setUrlId(event.getUrlId());
        analytics.setIpAddress(event.getIpAddress());
        analytics.setUserAgent(event.getUserAgent());
        analytics.setReferer(event.getReferer());
        
        analyticsRepository.save(analytics);
    }
}
