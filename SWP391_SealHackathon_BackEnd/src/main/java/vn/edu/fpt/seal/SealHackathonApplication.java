package vn.edu.fpt.seal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SealHackathonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SealHackathonApplication.class, args);
    }
}
