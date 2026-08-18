package com.portfolio.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class StatsResponseDto {
    private long totalClicks;
    private ZonedDateTime lastClickedAt;
    private List<ClickDetails> recentClicks;

    @Data
    @Builder
    public static class ClickDetails {
        private ZonedDateTime clickTime;
        private String browserOs;
        private String referer;
    }
}
