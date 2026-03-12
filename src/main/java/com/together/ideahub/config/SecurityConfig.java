package com.together.ideahub.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * idea-hub не валидирует JWT сам.
 * Gateway уже проверил токен и прокинул X-User-Id в заголовке.
 * Сервис доверяет этому заголовку — он доступен только через internal сеть.
 *
 * В prod: idea-hub должен быть закрыт от публичного доступа и доступен
 * только через Gateway (например, через внутреннюю Docker-сеть).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/categories", "/api/tags").permitAll()
                        .requestMatchers("GET", "/api/ideas", "/api/ideas/**").permitAll()
                        // Запись требует X-User-Id (проверяется в контроллере)
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
