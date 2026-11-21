package com.thanh.cinetogether.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClockConfig {

    // Code asks this bean for the time, so tests can control it.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

}
