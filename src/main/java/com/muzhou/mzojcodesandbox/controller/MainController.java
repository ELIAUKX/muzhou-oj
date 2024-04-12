package com.muzhou.mzojcodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
public class MainController {
    @GetMapping("/health")
    public String healthCheak() {
        return "ok";
    }
}
