package com.gametime.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GameTimeAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameTimeAdminApplication.class, args);
    }
}
