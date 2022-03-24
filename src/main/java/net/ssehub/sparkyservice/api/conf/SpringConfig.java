package net.ssehub.sparkyservice.api.conf;

import javax.sql.DataSource;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.user.storage.UserStorageImpl;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Default configuration class for spring.
 * 
 * @author marcel
 */
@Configuration
public class SpringConfig {

    public static final String LOCKED_JWT_BEAN = "lockedJwtToken";

    @Value("${db.user}")
    private String username;
    @Value("${db.password}")
    private String password;
    @Value("${db.addr:}")
    private String addr;
    @Value("${db.name:}")
    private String database;
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.driverClassName}")
    private String driver;
    
    /**
     * Configures the default spring datasource. It is used to read custom configuration keys from the context.
     */
    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(driver);
        String replacedUrl = String.format(url, addr, database);
        dataSourceBuilder.url(replacedUrl);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);
        return dataSourceBuilder.build();
    }

    /**
     * Defines the Validator bean (which is used for DTO validation).
     * 
     * @return Using {@link LocalValidatorFactoryBean}
     */
    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Defines the ZuulAuthroizationFilter bean.
     * 
     * @return Using {@link ZuulAuthorizationFilter}
     */
    @Bean
    public ZuulAuthorizationFilter zuulAuthorizationFilter() {
        return new ZuulAuthorizationFilter();
    }

    /**
     * Defines the IUserService bean.
     * 
     * @return Using {@link UserStorageImpl}
     */
    @Bean
    @Primary
    public UserStorageService iUserService() {
        return new UserStorageImpl();
    }
}
