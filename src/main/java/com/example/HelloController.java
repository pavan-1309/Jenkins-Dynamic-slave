package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    
    @GetMapping("/")
    public String hello() {
        return "Hello from Jenkins Docker Dynamic Slave!";
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
