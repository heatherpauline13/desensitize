package com.desensitize.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.desensitize.web",
        "com.desensitize.core",
        "com.desensitize.ai",
        "com.desensitize.annotation"
})
public class DesensitizeWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(DesensitizeWebApplication.class, args);
    }
}