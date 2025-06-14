package org.univcabi.univcabi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class UnivCabiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnivCabiApplication.class, args);
    }

}
