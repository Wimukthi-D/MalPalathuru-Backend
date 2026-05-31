package org.wimukthi.malpalathurubackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MalPalathuruBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MalPalathuruBackendApplication.class, args);
    }

}
