package net.ssehub.sparkyservice.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

//@ComponentScan(value = { "net.ssehub.sparkyservice.api", "net.ssehub.sparkyservice" })
//@SpringBootApplication(scanBasePackages="{net.ssehub.sparkyservice.api}")
@EntityScan("net.ssehub.sparkyservice.db")
//@EnableJpaRepositories("net.ssehub.sparkyservice.api")
//@EnableJpaRepositories
@SpringBootApplication
//@EnableAutoConfiguration
//@EnableTransactionManagement
public class SparkyServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SparkyServiceApplication.class, args);
    }
}
