package net.ssehub.sparkyservice.api.testconf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Annotations for integrations tests. Define behaviour here for all integration tests like default timeout. 
 * 
 * @author marcel
 */
@Test
@Retention(value = RetentionPolicy.RUNTIME)
@Timeout(180)
public @interface IntegrationTest {
    
}
