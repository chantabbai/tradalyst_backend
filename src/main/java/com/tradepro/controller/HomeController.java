package com.tradepro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String welcome() {
        return "TradePro API is running";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
} 