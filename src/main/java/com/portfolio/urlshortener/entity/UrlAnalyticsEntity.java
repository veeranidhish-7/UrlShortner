package com.portfolio.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.ZonedDateTime;

@Entity
@Table(name = "url_analytics")
@Getter
@Setter
public class UrlAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "click_time")
    private ZonedDateTime clickTime;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referer")
    private String referer;

    @PrePersist
    public void prePersist() {
        if (this.clickTime == null) {
            this.clickTime = ZonedDateTime.now();
        }
    }
}
