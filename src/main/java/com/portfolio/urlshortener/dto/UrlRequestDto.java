package com.portfolio.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UrlRequestDto {
    @NotBlank(message = "URL cannot be blank")
    @Pattern(regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", message = "Invalid URL format")
    private String originalUrl;

    @Pattern(regexp = "^$|^[a-zA-Z0-9_-]{2,30}$", message = "Custom alias must be 2-30 characters long and contain only letters, numbers, hyphens, and underscores")
    private String customAlias;
}
