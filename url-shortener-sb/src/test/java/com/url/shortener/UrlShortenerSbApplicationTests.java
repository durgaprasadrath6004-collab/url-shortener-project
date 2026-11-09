package com.url.shortener;

import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.service.UrlMappingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UrlShortenerSbApplicationTests {

    @Autowired
    private UrlMappingService urlMappingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testDeleteShortUrl() {
        User user = new User();
        user.setUsername("testuser_delete");
        user.setEmail("testdelete@example.com");
        user.setPassword("secret");
        user = userRepository.save(user);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl("https://example.com");
        urlMapping.setShortUrl("testdel1");
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());
        urlMapping = urlMappingRepository.save(urlMapping);

        ClickEvent click = new ClickEvent();
        click.setClickDate(LocalDateTime.now());
        click.setUrlMapping(urlMapping);
        clickEventRepository.save(click);

        boolean deleted = urlMappingService.deleteShortUrl(String.valueOf(urlMapping.getId()), user);
        assertTrue(deleted);
    }

}
