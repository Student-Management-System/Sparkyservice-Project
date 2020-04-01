package net.ssehub.sparkyservice.api.testconf;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import net.ssehub.sparkyservice.api.user.IUserService;
import net.ssehub.sparkyservice.api.user.UserServiceImpl;

/**
 * Spring configuration class which provides a set of beans which should be used during (unit) testing. 
 * In order to use it, use the following annotation: <br>
 * <code> @ContextConfiguration(classes= {UnitTestDataConfiguration.class}) </code>
 *
 * @author Marcel
 */
@TestConfiguration
public class UnitTestDataConfiguration {
       
    @Bean
    public IUserService iUserService() {
        return new UserServiceImpl();
    }
}