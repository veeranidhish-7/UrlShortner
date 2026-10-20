package com.portfolio.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Builder
public class AdminUrlDto {
    private Long id;
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Long clickCount;
    private Boolean isActive;
    private ZonedDateTime createdAt;
}
