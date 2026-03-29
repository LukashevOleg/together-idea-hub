package com.together.ideahub.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
     public CacheManager cacheManager() {
         CaffeineCacheManager manager = new CaffeineCacheManager();

         manager.registerCustomCache("weatherCache",
                 Caffeine.newBuilder()
                         .expireAfterWrite(Duration.ofMinutes(30))
                         .maximumSize(200)
                         .build()
         );

         manager.registerCustomCache("userPreferenceCache",
                 Caffeine.newBuilder()
                         .expireAfterWrite(Duration.ofMinutes(10))
                         .maximumSize(5000)
                         .build()
         );

         manager.registerCustomCache("geocodingCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofDays(1))
                        .maximumSize(1000)
                        .build());

         return manager;
     }
}
