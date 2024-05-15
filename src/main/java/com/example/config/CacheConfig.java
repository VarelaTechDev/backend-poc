package com.example.config;

import com.example.models.AssertionStartResponse;
import com.example.models.RegistrationStartResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, RegistrationStartResponse> registrationCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    @Bean
    public Cache<String, AssertionStartResponse> assertionCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }
}
