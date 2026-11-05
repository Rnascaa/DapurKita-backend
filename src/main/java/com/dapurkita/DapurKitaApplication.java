package com.dapurkita;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DapurKitaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DapurKitaApplication.class, args);
    }
}
