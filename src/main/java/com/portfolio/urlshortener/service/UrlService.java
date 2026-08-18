package com.portfolio.urlshortener.service;

import com.portfolio.urlshortener.entity.UrlEntity;
import com.portfolio.urlshortener.exception.UrlNotFoundException;
import com.portfolio.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Service base62Service;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_PREFIX = "urls::";
    private static final Duration REDIS_TTL = Duration.ofHours(24);

    @Transactional
    public String shortenUrl(String originalUrl) {
        UrlEntity urlEntity = new UrlEntity();
        urlEntity.setOriginalUrl(originalUrl);
        // Save first to get the generated sequence ID
        urlEntity = urlRepository.save(urlEntity);
        
        // Generate mathematically unique short code based on ID
        String shortCode = base62Service.encode(urlEntity.getId());
        urlEntity.setShortCode(shortCode);
        
        // Update entity with short code
        urlRepository.save(urlEntity);
        
        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        String redisKey = REDIS_PREFIX + shortCode;
        
        // Layer 1 (Redis Cache): Fast lookup
        String cachedUrl = redisTemplate.opsForValue().get(redisKey);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        // Layer 2 (PostgreSQL fallback): Read from DB on cache miss
        UrlEntity urlEntity = urlRepository.findByShortCode(shortCode)
                .filter(UrlEntity::getIsActive)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found or inactive"));

        // Put into Redis for next time
        redisTemplate.opsForValue().set(redisKey, urlEntity.getOriginalUrl(), REDIS_TTL);
        
        return urlEntity.getOriginalUrl();
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        UrlEntity urlEntity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));
        
        urlEntity.setIsActive(false);
        urlRepository.save(urlEntity);
        
        // Invalidate cache
        redisTemplate.delete(REDIS_PREFIX + shortCode);
    }
}
