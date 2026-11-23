package com.thanh.cinetogether.catalog.infrastructure;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thanh.cinetogether.catalog.domain.MovieProvider;

@Configuration(proxyBeanMethods = false)
class CatalogConfig {

    @Bean
    MovieProvider kkPhimProvider(@Value("${app.catalog.kkphim.base-url}") String baseUrl,
            @Value("${app.catalog.kkphim.connect-timeout}") Duration connectTimeout,
            @Value("${app.catalog.kkphim.read-timeout}") Duration readTimeout) {
        return new KKPhimProvider(baseUrl, connectTimeout, readTimeout);
    }

}
