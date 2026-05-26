package com.bistrodungu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.bistrodungu.shared",
    "com.bistrodungu.identity",
    "com.bistrodungu.menu",
    "com.bistrodungu.tables",
    "com.bistrodungu.reservations",
    "com.bistrodungu.orders",
    "com.bistrodungu.kds",
    "com.bistrodungu.inventory",
    "com.bistrodungu.billing",
    "com.bistrodungu.reporting"
})
public class BistroDunguApplication {

    public static void main(String[] args) {
        SpringApplication.run(BistroDunguApplication.class, args);
    }
}
