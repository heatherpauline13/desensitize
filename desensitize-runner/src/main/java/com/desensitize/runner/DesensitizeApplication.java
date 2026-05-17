package com.desensitize.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.desensitize")
public class DesensitizeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DesensitizeApplication.class, args);
    }
}
