package com.portfolio.urlshortener.controller;

import com.portfolio.urlshortener.dto.StatsResponseDto;
import com.portfolio.urlshortener.dto.UrlRequestDto;
import com.portfolio.urlshortener.entity.UrlAnalyticsEntity;
import com.portfolio.urlshortener.entity.UrlEntity;
import com.portfolio.urlshortener.exception.UrlNotFoundException;
import com.portfolio.urlshortener.repository.AnalyticsRepository;
import com.portfolio.urlshortener.repository.UrlRepository;
import com.portfolio.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For local development from index.html
public class UrlController {

    private final UrlService urlService;
    private final UrlRepository urlRepository;
    private final AnalyticsRepository analyticsRepository;

    @Value("${app.base-url:}")
    private String configuredBaseUrl;

    @PostMapping
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody UrlRequestDto requestDto, HttpServletRequest request) {
        String shortCode = urlService.shortenUrl(requestDto.getOriginalUrl());
        String baseUrl = resolveBaseUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("shortCode", shortCode, "shortUrl", baseUrl + "/" + shortCode));
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl.replaceAll("/+$", "");
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) {
            proto = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            int port = request.getServerPort();
            boolean isStandardPort = (port == 80 && "http".equalsIgnoreCase(proto)) || (port == 443 && "https".equalsIgnoreCase(proto));
            host = request.getServerName() + (isStandardPort ? "" : ":" + port);
        }
        return proto + "://" + host;
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<StatsResponseDto> getStats(@PathVariable String shortCode) {
        UrlEntity urlEntity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        List<UrlAnalyticsEntity> analytics = analyticsRepository
                .findByUrlIdAndClickTimeAfter(urlEntity.getId(), ZonedDateTime.now().minusDays(7));

        List<StatsResponseDto.ClickDetails> recentClicks = analytics.stream()
                .sorted((a, b) -> b.getClickTime().compareTo(a.getClickTime()))
                .limit(10)
                .map(a -> StatsResponseDto.ClickDetails.builder()
                        .clickTime(a.getClickTime())
                        .browserOs(a.getUserAgent() != null ? a.getUserAgent() : "Unknown")
                        .referer(a.getReferer() != null ? a.getReferer() : "Direct")
                        .build())
                .collect(Collectors.toList());

        ZonedDateTime lastClicked = analytics.stream()
                .map(UrlAnalyticsEntity::getClickTime)
                .max(ZonedDateTime::compareTo)
                .orElse(null);

        StatsResponseDto response = StatsResponseDto.builder()
                .totalClicks(urlEntity.getClickCount())
                .lastClickedAt(lastClicked)
                .recentClicks(recentClicks)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }
}
