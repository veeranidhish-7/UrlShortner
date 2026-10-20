package com.portfolio.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminSummaryDto {
    private long totalUrls;
    private long totalClicks;
    private long activeUrls;
    private List<AdminUrlDto> urls;
}
