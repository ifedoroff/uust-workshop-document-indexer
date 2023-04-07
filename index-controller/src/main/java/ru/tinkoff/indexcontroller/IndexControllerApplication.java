package ru.tinkoff.indexcontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IndexControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndexControllerApplication.class, args);
    }

}
