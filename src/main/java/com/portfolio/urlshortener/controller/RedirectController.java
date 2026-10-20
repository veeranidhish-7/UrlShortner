package com.portfolio.urlshortener.controller;

import com.portfolio.urlshortener.event.UrlClickEvent;
import com.portfolio.urlshortener.repository.UrlRepository;
import com.portfolio.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;
    private final UrlRepository urlRepository;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        // Resolve URL (hits Redis cache first, then DB)
        String originalUrl = urlService.getOriginalUrl(shortCode);

        // Fire and forget telemetry event asynchronously
        urlRepository.findByShortCode(shortCode).ifPresent(urlEntity -> {
            UrlClickEvent clickEvent = new UrlClickEvent(
                    this,
                    urlEntity.getId(),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"),
                    request.getHeader("Referer")
            );
            eventPublisher.publishEvent(clickEvent);
        });

        // 302 Found redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}
