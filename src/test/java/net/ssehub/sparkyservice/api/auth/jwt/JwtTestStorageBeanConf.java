package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtRepository;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtStorageService;
import net.ssehub.sparkyservice.api.persistence.TestingUserRepository;
import net.ssehub.sparkyservice.api.persistence.UserDatabaseStorageService;
import net.ssehub.sparkyservice.api.persistence.UserStorageService;

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

    @Bean
    public JwtStorageService storageService(UserStorageService userStorageServ) {
        assertNotNull(repo);
        assertNotNull(userStorageServ);
        return new JwtStorageService(notNull(repo), notNull(userStorageServ));
    }
    
    @Bean
    public UserStorageService userService(@Nonnull TestingUserRepository repo) {
        return new UserDatabaseStorageService(repo);
    }
}
