package com.thanh.cinetogether.catalog.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thanh.cinetogether.catalog.application.Catalog;
import com.thanh.cinetogether.catalog.domain.browse.Home;

@RestController
@RequestMapping("/api/v1/catalog")
class HomeController {

    private final Catalog catalog;

    HomeController(Catalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/home")
    Home home() {
        return catalog.home();
    }

}
