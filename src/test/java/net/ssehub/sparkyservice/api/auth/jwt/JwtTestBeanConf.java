package net.ssehub.sparkyservice.api.auth.jwt;

import java.util.Base64;

import javax.annotation.Nonnull;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.config.ConfigurationValues.JwtSettings;


@TestConfiguration
public class JwtTestBeanConf {

    @Bean
    public JwtSettings sampleJwtConf() {
        JwtSettings jwtConf = new JwtSettings();
        var secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String secretString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        jwtConf.setSecret(secretString);
        jwtConf.setAudience("Y");
        jwtConf.setHeader("Authorization");
        jwtConf.setIssuer("TEST");
        jwtConf.setType("Bearer");
        return jwtConf;
    }
    
    @Bean
    public JwtTokenService tokenService(JwtSettings conf) {
        return new JwtTokenService(conf);
    }
    
    @Bean
    public JwtAuthReader authReader(@Nonnull JwtTokenService service, @Nonnull JwtSettings conf) {
        return new JwtAuthReader(service, conf);
    }
}
