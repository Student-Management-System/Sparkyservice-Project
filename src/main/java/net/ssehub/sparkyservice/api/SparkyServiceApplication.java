package net.ssehub.sparkyservice.api;

import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Main class. 
 * 
 * @author marcel
 */
@EntityScan("net.ssehub.sparkyservice.api.jpa")
@SpringBootApplication
@EnableZuulProxy
public class SparkyServiceApplication {
    
    /**
     * Main method which starts the spring boot Tomcat. 
     * 
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(SparkyServiceApplication.class, args);
    }
}
