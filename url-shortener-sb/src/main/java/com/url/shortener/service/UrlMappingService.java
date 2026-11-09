package com.url.shortener.service;

import com.url.shortener.dtos.ClickEventDTO;
import com.url.shortener.dtos.UrlMappingDTO;
import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UrlMappingService {

    private static final Logger log = LoggerFactory.getLogger(UrlMappingService.class);

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;
    private final StringRedisTemplate redisTemplate;

    public UrlMappingService(UrlMappingRepository urlMappingRepository,
                             ClickEventRepository clickEventRepository,
                             @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickEventRepository = clickEventRepository;
        this.redisTemplate = redisTemplate;
    }

    public UrlMappingDTO createShortUrl(String originalUrl, User user) {
        if (originalUrl != null) {
            originalUrl = originalUrl.trim();
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "https://" + originalUrl;
            }
        }
        String shortUrl = generateShortUrl();
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());
        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);

        // Cache in Redis
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("short:" + shortUrl, originalUrl, 7, TimeUnit.DAYS);
                log.info("Cached shortUrl [{}] to originalUrl [{}] in Redis", shortUrl, originalUrl);
            } catch (Exception e) {
                log.warn("Failed to cache shortUrl in Redis: {}", e.getMessage());
            }
        }

        return convertToDto(savedUrlMapping);
    }

    private UrlMappingDTO convertToDto(UrlMapping urlMapping){
        UrlMappingDTO urlMappingDTO = new UrlMappingDTO();
        urlMappingDTO.setId(urlMapping.getId());
        urlMappingDTO.setOriginalUrl(urlMapping.getOriginalUrl());
        urlMappingDTO.setShortUrl(urlMapping.getShortUrl());
        urlMappingDTO.setClickCount(urlMapping.getClickCount());
        urlMappingDTO.setCreatedDate(urlMapping.getCreatedDate());
        urlMappingDTO.setUsername(urlMapping.getUser().getUsername());
        return urlMappingDTO;
    }

    private String generateShortUrl() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        Random random = new Random();
        StringBuilder shortUrl = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            shortUrl.append(characters.charAt(random.nextInt(characters.length())));
        }
        return shortUrl.toString();
    }

    public List<UrlMappingDTO> getUrlsByUser(User user) {
        return urlMappingRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .toList();
    }

    public List<ClickEventDTO> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        if (urlMapping != null) {
            return clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping, start, end).stream()
                    .collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> {
                        ClickEventDTO clickEventDTO = new ClickEventDTO();
                        clickEventDTO.setClickDate(entry.getKey());
                        clickEventDTO.setCount(entry.getValue());
                        return clickEventDTO;
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingInAndClickDateBetween(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return clickEvents.stream()
                .collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()));

    }

    public UrlMapping getOriginalUrl(String shortUrl) {
        // Check Redis cache first
        String cachedOriginalUrl = null;
        if (redisTemplate != null) {
            try {
                cachedOriginalUrl = redisTemplate.opsForValue().get("short:" + shortUrl);
                if (cachedOriginalUrl != null) {
                    log.info("Redis cache hit for shortUrl [{}] -> [{}]", shortUrl, cachedOriginalUrl);
                }
            } catch (Exception e) {
                log.warn("Redis lookup failed, falling back to database: {}", e.getMessage());
            }
        }

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        if (urlMapping != null) {
            // Populate Redis if cache miss
            if (redisTemplate != null && cachedOriginalUrl == null) {
                try {
                    redisTemplate.opsForValue().set("short:" + shortUrl, urlMapping.getOriginalUrl(), 7, TimeUnit.DAYS);
                } catch (Exception ignored) {
                }
            }

            urlMapping.setClickCount(urlMapping.getClickCount() + 1);
            urlMappingRepository.save(urlMapping);

            // Record Click Event
            ClickEvent clickEvent = new ClickEvent();
            clickEvent.setClickDate(LocalDateTime.now());
            clickEvent.setUrlMapping(urlMapping);
            clickEventRepository.save(clickEvent);
        }

        return urlMapping;
    }

    @Transactional
    public boolean deleteShortUrl(String identifier, User user) {
        UrlMapping urlMapping = null;
        try {
            Long id = Long.parseLong(identifier);
            urlMapping = urlMappingRepository.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {
        }
        if (urlMapping == null) {
            urlMapping = urlMappingRepository.findByShortUrl(identifier);
        }
        if (urlMapping == null) {
            log.warn("UrlMapping not found for deletion identifier: {}", identifier);
            return false;
        }
        if (urlMapping.getUser() != null && user != null && !urlMapping.getUser().getId().equals(user.getId())) {
            log.warn("User [{}] unauthorized to delete URL [{}] owned by [{}]",
                    user.getUsername(), identifier, urlMapping.getUser().getUsername());
            throw new RuntimeException("Unauthorized to delete this URL");
        }
        String shortUrl = urlMapping.getShortUrl();

        // Evict from Redis cache
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("short:" + shortUrl);
                log.info("Evicted deleted shortUrl [{}] from Redis", shortUrl);
            } catch (Exception e) {
                log.warn("Failed to evict shortUrl from Redis: {}", e.getMessage());
            }
        }

        // Delete click events and mapping
        try {
            clickEventRepository.deleteByUrlMapping(urlMapping);
        } catch (Exception e) {
            log.warn("deleteByUrlMapping exception: {}", e.getMessage());
        }
        urlMappingRepository.delete(urlMapping);
        log.info("Deleted shortUrl [{}] for user [{}]", shortUrl, user != null ? user.getUsername() : "unknown");
        return true;
    }
}
