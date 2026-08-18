package com.portfolio.urlshortener.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UrlClickEvent extends ApplicationEvent {
    
    private final Long urlId;
    private final String ipAddress;
    private final String userAgent;
    private final String referer;

    public UrlClickEvent(Object source, Long urlId, String ipAddress, String userAgent, String referer) {
        super(source);
        this.urlId = urlId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }
}
