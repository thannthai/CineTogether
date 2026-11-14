package com.thanh.cinetogether;

import org.springframework.boot.SpringApplication;

public class TestCinetogetherApplication {

    public static void main(String[] args) {
        SpringApplication.from(CinetogetherApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
