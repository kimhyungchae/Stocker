package com.atis.stocker.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@Configuration
@EnableJpaRepositories(basePackages = "com.atis.*")
@EntityScan(basePackages="com.atis.*")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}