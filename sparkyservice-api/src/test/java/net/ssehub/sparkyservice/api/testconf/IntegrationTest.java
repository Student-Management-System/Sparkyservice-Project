package net.ssehub.sparkyservice.api.testconf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

@Test
@Retention(value = RetentionPolicy.RUNTIME)
public @interface IntegrationTest {}
