package net.ssehub.sparkyservice.api.user.modification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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
        return new AdminUserModificationImpl();
    }
}