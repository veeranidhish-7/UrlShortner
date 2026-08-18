package com.portfolio.urlshortener.event;

import com.portfolio.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlClickEventListener {

    private final AnalyticsService analyticsService;

    @Async
    @EventListener
    public void handleUrlClickEvent(UrlClickEvent event) {
        analyticsService.processClickEvent(event);
    }
}
