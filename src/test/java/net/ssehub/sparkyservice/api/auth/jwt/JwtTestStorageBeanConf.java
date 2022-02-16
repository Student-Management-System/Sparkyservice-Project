package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtRepository;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtStorageService;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Provides JWT bean definition for JWT services. 
 * When using this, enable repository scan: 
 * <br><br>
 * <code> @EnableJpaRepositories("net.ssehub.sparkyservice.api") </code> as class annotation.
 * 
 * @author marcel
 */
@TestConfiguration
public class JwtTestStorageBeanConf extends JwtTestBeanConf {

    @Autowired 
    private JwtRepository repo;

    @Autowired 
    private UserStorageService userStorageServ;
    
    @Bean
    public JwtStorageService storageService() {
        assertNotNull(repo);
        assertNotNull(userStorageServ);
        return new JwtStorageService(notNull(repo), notNull(userStorageServ));
    }
}
