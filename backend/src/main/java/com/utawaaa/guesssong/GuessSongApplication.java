package com.utawaaa.guesssong;

import com.utawaaa.guesssong.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class GuessSongApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuessSongApplication.class, args);
    }
}
