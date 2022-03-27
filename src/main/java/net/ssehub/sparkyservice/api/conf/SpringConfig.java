package net.ssehub.sparkyservice.api.conf;

import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

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
    private static final String dateFormat = "yyyy-MM-dd";
    private static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";

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
    
    /**
     * Creates a jackson mapper for consitent usage of the same json date pattern. Move this to the application 
     * properties as soon the used spring version supports it: 
     * See <a href="https://www.baeldung.com/spring-boot-formatting-json-dates">this</a>. 
     * 
     * @return
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.simpleDateFormat(dateTimeFormat);
            var formatterDateTime = DateTimeFormatter.ofPattern(dateTimeFormat);
            var formatterDate = DateTimeFormatter.ofPattern(dateFormat);
            builder.serializers(new LocalDateSerializer(formatterDate));
            builder.deserializers(new LocalDateDeserializer(formatterDate));
            builder.serializers(new LocalDateTimeSerializer(formatterDateTime));
            builder.deserializers(new LocalDateTimeDeserializer(formatterDateTime));
        };
    }
}
