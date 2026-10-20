package com.portfolio.urlshortener.controller;

import com.portfolio.urlshortener.dto.AdminSummaryDto;
import com.portfolio.urlshortener.dto.AdminUrlDto;
import com.portfolio.urlshortener.entity.UrlEntity;
import com.portfolio.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/urls")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UrlService urlService;

    @Value("${app.admin-key:adminNidhish}")
    private String configuredAdminKey;

    @Value("${app.base-url:}")
    private String configuredBaseUrl;

    private boolean isAuthorized(String adminKey) {
        if (adminKey == null || adminKey.isBlank()) {
            return false;
        }
        return adminKey.trim().equals(configuredAdminKey.trim());
    }

    @GetMapping
    public ResponseEntity<?> getAllUrls(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            HttpServletRequest request
    ) {
        if (!isAuthorized(adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: Invalid or missing admin key"));
        }

        List<UrlEntity> allUrls = urlService.getAllUrls();
        String baseUrl = resolveBaseUrl(request);

        long totalClicks = allUrls.stream().mapToLong(u -> u.getClickCount() != null ? u.getClickCount() : 0L).sum();
        long activeCount = allUrls.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();

        List<AdminUrlDto> dtoList = allUrls.stream().map(u -> AdminUrlDto.builder()
                .id(u.getId())
                .shortCode(u.getShortCode())
                .shortUrl(baseUrl + "/" + u.getShortCode())
                .originalUrl(u.getOriginalUrl())
                .clickCount(u.getClickCount() != null ? u.getClickCount() : 0L)
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        AdminSummaryDto summary = AdminSummaryDto.builder()
                .totalUrls(allUrls.size())
                .totalClicks(totalClicks)
                .activeUrls(activeCount)
                .urls(dtoList)
                .build();

        return ResponseEntity.ok(summary);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<?> deleteUrl(
            @PathVariable String shortCode,
            @RequestParam(value = "purge", defaultValue = "true") boolean purge,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        if (!isAuthorized(adminKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: Invalid or missing admin key"));
        }

        if (purge) {
            urlService.purgeUrl(shortCode);
        } else {
            urlService.deleteUrl(shortCode);
        }

        return ResponseEntity.noContent().build();
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
}
