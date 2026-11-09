package com.url.shortener.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/urlshortener}")
    private String dbUrl;

    @Value("${spring.datasource.username:postgres}")
    private String dbUsername;

    @Value("${spring.datasource.password:postgres}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String url = dbUrl;
        String username = dbUsername;
        String password = dbPassword;

        try {
            if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
                URI uri = new URI(url);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String database = uri.getPath();
                if (database != null && database.startsWith("/")) {
                    database = database.substring(1);
                }

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    username = userInfo[0];
                    if (userInfo.length > 1) {
                        password = userInfo[1];
                    }
                }

                url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                    url = url + "?" + uri.getQuery();
                }
                log.info("Auto-converted Render database URL to JDBC format: jdbc:postgresql://{}:{}/{}", host, port, database);
            }
        } catch (Exception e) {
            log.warn("Could not parse database URL as URI: {}", e.getMessage());
        }

        config.setJdbcUrl(url);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        config.setDriverClassName("org.postgresql.Driver");

        return new HikariDataSource(config);
    }
}

