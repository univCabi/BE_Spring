package org.univcabi.univcabi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // Spring Data Jpa의 Auditing 기능 활성화 ( 감사 기능 )
public class UnivCabiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnivCabiApplication.class, args);
    }

}
