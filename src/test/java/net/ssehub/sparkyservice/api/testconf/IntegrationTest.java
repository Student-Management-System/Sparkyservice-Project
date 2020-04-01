package net.ssehub.sparkyservice.api.testconf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Test
@Retention(value = RetentionPolicy.RUNTIME)
@Timeout(300)
public @interface IntegrationTest {}
