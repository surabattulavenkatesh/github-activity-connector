package com.example.githubconnector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${github.api.token}")
    private String githubToken;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        if (!StringUtils.hasText(githubToken)) {
            // Fail fast if the token is not configured
            throw new IllegalStateException("GitHub API token is not configured. Please set GITHUB_TOKEN environment variable.");
        }
        return builder
            .defaultHeader("Authorization", "token " + githubToken)
            .defaultHeader("Accept", "application/vnd.github.v3+json")
            .build();
    }
}