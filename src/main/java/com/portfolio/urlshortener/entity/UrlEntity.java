package com.portfolio.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.ZonedDateTime;

@Entity
@Table(name = "urls")
@Getter
@Setter
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", length = 10, unique = true)
    private String shortCode;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "click_count")
    private Long clickCount = 0L;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
        if (this.clickCount == null) {
            this.clickCount = 0L;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}
