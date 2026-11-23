package com.thanh.cinetogether.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Web slice with {@link SecurityConfig} only. {@code @WebMvcTest} would load every controller, but these tests are
 * about security and have no database, so feature modules are kept out.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@WebMvcTest(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
        pattern = "com\\.thanh\\.cinetogether\\.(identity|catalog)\\..*"))
@Import(SecurityConfig.class)
@interface SecurityWebMvcTest {
}
