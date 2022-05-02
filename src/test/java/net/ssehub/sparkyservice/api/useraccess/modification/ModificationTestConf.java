package net.ssehub.sparkyservice.api.useraccess.modification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.local.LocalRealm;

/**
 * Because we want to use <code>@Autowired</code> in unit tests, we need to
 * extend the test class with {@link SpringExtension}. But we're not in a
 * complete spring environment and {@link Service} aren't loaded. So we need to
 * provide all beans which are used manually.
 *
 * @author marcel
 */
@TestConfiguration
public class ModificationTestConf {

    /**
     * Bean definition.
     *
     * @return .
     */
    @Bean
    public AdminUserModificationImpl adminModificationService() {
        return new AdminUserModificationImpl(new DefaultUserModificationImpl());
    }
    
    @Bean
    public DefaultUserModificationImpl defaultModificationService() {
        return new DefaultUserModificationImpl();
    }
    
    @Bean
    public LocalRealm localRealm() {
        return new LocalRealm();
    }
}