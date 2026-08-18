package com.portfolio.urlshortener.controller;

import com.portfolio.urlshortener.dto.StatsResponseDto;
import com.portfolio.urlshortener.dto.UrlRequestDto;
import com.portfolio.urlshortener.entity.UrlAnalyticsEntity;
import com.portfolio.urlshortener.entity.UrlEntity;
import com.portfolio.urlshortener.exception.UrlNotFoundException;
import com.portfolio.urlshortener.repository.AnalyticsRepository;
import com.portfolio.urlshortener.repository.UrlRepository;
import com.portfolio.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody UrlRequestDto requestDto) {
        String shortCode = urlService.shortenUrl(requestDto.getOriginalUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("shortCode", shortCode, "shortUrl", "http://localhost:8080/" + shortCode));
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
