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

    private static final java.util.Set<String> RESERVED_KEYWORDS = java.util.Set.of(
            "api", "admin", "login", "register", "stats", "index.html", "style.css", "app.js", "favicon.ico", "error"
    );

    @PostMapping
    public ResponseEntity<?> createShortUrl(
            @Valid @RequestBody UrlRequestDto requestDto,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            HttpServletRequest request
    ) {
        String customAlias = requestDto.getCustomAlias();
        if (customAlias != null && !customAlias.isBlank()) {
            customAlias = customAlias.trim();
            // 1. Verify admin privilege
            if (adminKey == null || !adminKey.trim().equals(configuredAdminKey.trim())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only Admin has the privilege to create custom short codes!"));
            }
            // 2. Check reserved keywords
            if (RESERVED_KEYWORDS.contains(customAlias.toLowerCase())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "The alias '" + customAlias + "' is a reserved keyword"));
            }
            // 3. Check for existing alias
            if (urlRepository.findByShortCode(customAlias).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Custom alias '" + customAlias + "' is already in use!"));
            }
        }

        String shortCode = urlService.shortenUrl(requestDto.getOriginalUrl(), customAlias);
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

    @Value("${app.admin-key:adminNidhish}")
    private String configuredAdminKey;

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<?> deleteUrl(
            @PathVariable String shortCode,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        if (adminKey == null || !adminKey.trim().equals(configuredAdminKey.trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: Admin privileges required to delete links"));
        }
        urlService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }
}
