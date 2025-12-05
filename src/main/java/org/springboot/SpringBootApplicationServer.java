package org.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.springboot", "org.matsim"})
public class SpringBootApplicationServer {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApplicationServer.class, args);
    }
}